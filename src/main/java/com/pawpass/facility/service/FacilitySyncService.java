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
import java.util.List;

/**
 * 한국문화정보원 반려동물 동반 가능 문화시설 데이터를 전량 동기화한다.
 * 원본 API에 지역/카테고리 필터가 없어서 매번 전체(약 70,650건)를 페이지네이션으로 순회하고,
 * "반려동물 동반 가능정보" = Y인 것만 저장한다 (N도 섞여서 내려오는 원본 데이터셋 특성).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacilitySyncService {

    // TODO: odcloud.kr 실제 perPage 상한 미확인. 로컬에서 실제 키로 한 번 호출해보고 필요하면 낮출 것
    private static final int PAGE_SIZE = 1000;

    private final KcisaApiClient kcisaApiClient;
    private final PetFacilityRepository petFacilityRepository;

    public void syncAll() {
        int page = 1;
        int totalSaved = 0;

        while (true) {
            KcisaFacilityListResponse response = kcisaApiClient.fetchPage(page, PAGE_SIZE);
            List<KcisaFacilityItem> items = response.data();
            if (items == null || items.isEmpty()) {
                break;
            }

            for (KcisaFacilityItem item : items) {
                if (!"Y".equalsIgnoreCase(item.petAllowed())) {
                    continue;
                }
                petFacilityRepository.save(toEntity(item));
                totalSaved++;
            }

            Integer totalCount = response.totalCount();
            if (totalCount == null || (long) page * PAGE_SIZE >= totalCount) {
                break;
            }
            page++;
        }

        log.info("한국문화정보원 반려동물 동반 시설 동기화 완료 - {}건 저장", totalSaved);
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
