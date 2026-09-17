package com.pawpass.facility.service;

import com.pawpass.facility.client.KcisaApiClient;
import com.pawpass.facility.domain.PetFacility;
import com.pawpass.facility.dto.external.KcisaFacilityItem;
import com.pawpass.facility.dto.external.KcisaFacilityListResponse;
import com.pawpass.facility.repository.PetFacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 한국문화정보원 반려동물 동반 가능 문화시설 데이터를 전량 동기화한다.
 * 원본 API에 지역/카테고리 필터도, 변경분만 조회하는 기능(updated-since 등)도 없어서 매번 전체
 * (약 70,650건)를 페이지네이션으로 순회해야 한다 - 이건 API 자체의 한계라 줄일 방법이 없다.
 * 다만 예전엔 매번 전체 건을 무조건 DB에 다시 write했는데(변경 여부와 무관하게), 그건 API 제약과 무관한
 * 자체 비효율이었어서 이번에 고쳤다: 페이지 단위로 기존 데이터를 한 번에 조회해 원본의 "최종작성일"
 * (issuedDate)이 그대로면 write 자체를 건너뛰고, 바뀐 것만 saveAll로 묶어서 쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacilitySyncService {

    // 실제 키로 perPage 1000~5000 구간을 실측: 고정된 건수 상한이 아니라 응답 payload 크기 상한(약 4MB 부근)이다.
    // perPage=3800(≈4.16MB)까지는 정상 응답, perPage=3900부터는 데이터 없이 {"code":0,"msg":"정상"}만 내려옴.
    // 현재 값(perPage=1000, ≈1.1MB)은 그 상한에서 충분히 여유 있어 그대로 유지.
    private static final int PAGE_SIZE = 1000;


    private static final int MAX_EMPTY_PAGE_RETRIES = 3;
    private static final long EMPTY_PAGE_RETRY_DELAY_MS = 2000;

    private final KcisaApiClient kcisaApiClient;
    private final PetFacilityRepository petFacilityRepository;

    public void syncAll() {
        int page = 1;
        int totalSaved = 0;
        int totalUnchanged = 0;
        Integer totalCount = null;
        boolean truncated = false;

        while (true) {
            KcisaFacilityListResponse response = fetchPageWithRetry(page, totalCount);
            totalCount = response.totalCount();
            List<KcisaFacilityItem> items = response.data();

            boolean coveredEverything = totalCount != null && (long) (page - 1) * PAGE_SIZE >= totalCount;
            if (items == null || items.isEmpty()) {
                if (!coveredEverything) {
                    // totalCount에 한참 못 미쳤는데 재시도 끝에도 빈 응답 - 진짜 끝이 아니라 중단된 것이다.
                    truncated = true;
                }
                break;
            }

            // 이 페이지에서 반려동물 동반 가능(Y)인 것만 후보로 만든다 - id는 title+address 해시라 미리 계산 가능
            Map<String, PetFacility> candidates = new LinkedHashMap<>();
            for (KcisaFacilityItem item : items) {
                if (!"Y".equalsIgnoreCase(item.petAllowed())) {
                    continue;
                }
                PetFacility fresh = toEntity(item);
                candidates.put(fresh.getId(), fresh);
            }

            // 페이지당 한 번의 벌크 조회로 기존 값을 가져와서, 후보마다 매번 findById를 날리지 않는다
            Map<String, PetFacility> existingById = petFacilityRepository.findAllById(candidates.keySet()).stream()
                    .collect(Collectors.toMap(PetFacility::getId, f -> f));

            List<PetFacility> toSave = new ArrayList<>();
            for (Map.Entry<String, PetFacility> entry : candidates.entrySet()) {
                PetFacility existing = existingById.get(entry.getKey());
                PetFacility fresh = entry.getValue();
                if (existing != null && Objects.equals(existing.getIssuedDate(), fresh.getIssuedDate())) {
                    totalUnchanged++;
                    continue;
                }
                // googlePlaceId는 이 배치가 아니라 FacilityService가 조회 시점에 지연 해소한다(known-not-found면
                // ""로 캐싱) - 여기서 fresh를 통째로 덮어쓸 때 이어받지 않으면 데이터가 바뀔 때마다 이미
                // 해소해둔 값이 날아가서 또 조회 시점에 다시 찾아야 한다. 있으면 그대로 이어받는다.
                if (existing != null) {
                    fresh.setGooglePlaceId(existing.getGooglePlaceId());
                }
                toSave.add(fresh);
            }

            petFacilityRepository.saveAll(toSave);
            totalSaved += toSave.size();

            if (totalCount == null || (long) page * PAGE_SIZE >= totalCount) {
                break;
            }
            page++;
        }

        if (truncated) {
            log.warn("한국문화정보원 반려동물 동반 시설 동기화 중단됨 - {}건 저장(신규/변경), {}건 변경없음 스킵, "
                            + "페이지 {}에서 예상(totalCount={})보다 일찍 빈 응답을 받아 재시도({}회)했지만 실패함. "
                            + "다음 스케줄 실행 때 다시 시도됨 - 계속 반복되면 원본 API 자체 점검 필요.",
                    totalSaved, totalUnchanged, page, totalCount, MAX_EMPTY_PAGE_RETRIES);
        } else {
            log.info("한국문화정보원 반려동물 동반 시설 동기화 완료 - {}건 저장(신규/변경), {}건 변경없음 스킵 (totalCount={})",
                    totalSaved, totalUnchanged, totalCount);
        }
    }

    /**
     * totalCount 기준으로 아직 다 순회하지 못한 게 확실한 상황에서 빈 응답이 오면, 진짜 끝이 아니라
     * 일시적 문제로 보고 재시도한다. 첫 페이지(totalCount를 아직 모름)는 무조건 그대로 반환 - 비교할
     * 기준 자체가 없어서 재시도 여부를 판단할 수 없다.
     */
    private KcisaFacilityListResponse fetchPageWithRetry(int page, Integer knownTotalCount) {
        KcisaFacilityListResponse response = kcisaApiClient.fetchPage(page, PAGE_SIZE);
        if (knownTotalCount == null) {
            return response;
        }
        boolean coveredEverything = (long) (page - 1) * PAGE_SIZE >= knownTotalCount;
        for (int attempt = 1;
             attempt <= MAX_EMPTY_PAGE_RETRIES && !coveredEverything && isEmpty(response);
             attempt++) {
            log.warn("페이지 {} 응답이 비어있음(totalCount={}, 아직 다 순회 못함) - {}번째 재시도", page, knownTotalCount, attempt);
            sleep(EMPTY_PAGE_RETRY_DELAY_MS);
            response = kcisaApiClient.fetchPage(page, PAGE_SIZE);
        }
        return response;
    }

    private boolean isEmpty(KcisaFacilityListResponse response) {
        return response.data() == null || response.data().isEmpty();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private PetFacility toEntity(KcisaFacilityItem item) {
        String address = hasText(item.roadAddress()) ? item.roadAddress() : item.lotAddress();

        return PetFacility.builder()
                .id(generateId(item.title(), address))
                .title(item.title())
                .category1(item.category1())
                .category2(item.category2())
                .category3(item.category3())
                .address(address)
                .zipcode(item.zipcode() != null ? String.format("%05d", item.zipcode()) : null)
                .lat(parseDouble(item.lat()))
                .lng(parseDouble(item.lng()))
                .tel(item.tel())
                .url(item.homepage())
                .charge(item.charge())
                .operatingHours(item.operatingHours())
                .closedDays(item.closedDays())
                .parkingAvailable(parseYn(item.parkingAvailable()))
                .petAllowed(parseYn(item.petAllowed()))
                .petExclusive(item.petExclusive())
                .allowedPetSize(item.allowedPetSize())
                .petRestriction(item.petRestriction())
                .indoor(parseYn(item.indoor()))
                .outdoor(parseYn(item.outdoor()))
                .additionalPetFee(item.additionalPetFee())
                .descriptionRaw(item.description())
                .issuedDate(item.issuedDate())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Double parseDouble(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseYn(String value) {
        if ("Y".equalsIgnoreCase(value)) return true;
        if ("N".equalsIgnoreCase(value)) return false;
        return null;
    }

    /** 원본 API에 고유 식별자가 없어 title+address 해시로 자체 생성 (PetFacility.id 주석 참고) */
    private String generateId(String title, String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((title + "|" + address).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
