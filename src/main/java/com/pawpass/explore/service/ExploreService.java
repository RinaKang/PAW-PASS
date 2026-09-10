package com.pawpass.explore.service;

import com.pawpass.explore.dto.ExploreItem;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.matching.dto.MatchResponse;
import com.pawpass.matching.service.MatchingService;
import com.pawpass.pet.domain.Pet;
import com.pawpass.tour.service.TourService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.pawpass.facility.dto.FacilitySummaryResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * tour(TourAPI 실시간) + facility(KCISA DB) 검색 결과를 합쳐서 반환한다.
 * 같은 장소가 양쪽에 다 있으면 tourapi 쪽을 우선 노출한다 (README 명세).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreService {

    /** 두 소스 좌표가 이 거리(m) 이내면 같은 장소로 본다 - 서로 다른 데이터셋의 좌표 오차를 감안한 여유값 */
    private static final double DEDUP_DISTANCE_METERS = 50.0;

    private static final String DEFAULT_MATCH_STATUS = "확인필요";

    /**
     * "초기 목록엔 가능·조건부만 표시". petId가 있는데 matchStatus를
     * 명시적으로 안 넘긴 "기본" 요청에서만 서버가 이 필터를 강제한다 - 프론트가 matchStatus를 명시하면
     * (불가/확인필요 포함) 그 값 그대로 필터링해서 보여준다.
     */
    private static final Set<String> DEFAULT_VISIBLE_STATUSES = Set.of(
            MatchResponse.STATUS_ALLOWED, MatchResponse.STATUS_CONDITIONAL
    );

    /**
     * Gemini 무료 티어가 분당 5회로 요청을 제한한다  동시 실행 수를 낮춘다고 분당 총량 제한 자체가 없어지진 않으므로(그래서 완전한 해결책은
     * 아님), 순간 버스트로 즉시 다 막히는 것만이라도 줄이는 용도 - 진짜 해결은 아래 computeOne()의
     * "실패하면 확인필요로 대체" 쪽이다.
     */
    private static final int MATCH_CONCURRENCY = 3;

    private final TourService tourService;
    private final FacilityService facilityService;
    private final MatchingService matchingService;
    // 데몬 스레드로 만들어서, 테스트처럼 Spring 컨텍스트 없이 직접 생성했다가 shutdown()을 안 부르는
    // 경우에도(@PreDestroy는 Spring이 관리할 때만 호출됨) 이 스레드들 때문에 JVM 종료가 막히지 않게 한다.
    private final ExecutorService matchExecutor = Executors.newFixedThreadPool(MATCH_CONCURRENCY, runnable -> {
        Thread thread = new Thread(runnable, "explore-match");
        thread.setDaemon(true);
        return thread;
    });

    @PreDestroy
    void shutdown() {
        matchExecutor.shutdown();
    }

    /**
     * petId가 없으면(반려동물을 아직 등록 안 한 사용자 등) 개인화 판정 없이 전부 "확인필요"로 내려가고,
     * matchStatus 기본 필터는 적용하지 않는다(적용하면 전부 걸러져서 빈 목록만 나옴 - 의미 없음).
     * petId가 있으면 항목마다 실제 매칭(TourAPI 실시간 조회 + 규칙/AI 판정)을 돌려서 진짜 match_status를
     * 채우고, matchStatus를 명시하지 않은 "기본" 요청은 가능/조건부만 반환한다
     */
    public List<ExploreItem> explore(Long userId, String regionCode, String category, String matchStatus, Long petId, int page) {
        List<ExploreItem> tourItems = searchTourItems(regionCode, category, page);
        List<ExploreItem> facilityItems = searchFacilityItems(regionCode, category, page);

        List<ExploreItem> merged = mergeTourApiFirst(tourItems, facilityItems);

        boolean personalized = petId != null;
        if (personalized) {
            Pet pet = matchingService.requireOwnedPet(userId, petId);
            merged = computeMatchStatuses(merged, pet);
        }

        if (matchStatus != null && !matchStatus.isBlank()) {
            return merged.stream()
                    .filter(item -> matchStatus.equals(item.matchStatus()))
                    .toList();
        }
        if (personalized) {
            return merged.stream()
                    .filter(item -> DEFAULT_VISIBLE_STATUSES.contains(item.matchStatus()))
                    .toList();
        }
        return merged;
    }

    /**
     * 공통 지역명을 tour 쪽 규격(lDongRegnCd/lDongSignguCd)으로 바꿔서 조회한다.
     * 매핑 테이블에 없는 지역/카테고리는 ExploreConditionMapper가 필터 없음(Optional.empty)으로 돌려주므로
     * 자동으로 전체 조회에 가깝게 동작한다 - 값 하나 잘못 왔다고 빈 목록이 되진 않는다.
     */
    private List<ExploreItem> searchTourItems(String regionCode, String category, int page) {
        ExploreConditionMapper.TourRegion region = ExploreConditionMapper.toTourRegion(regionCode).orElse(null);
        String lDongRegnCd = region == null ? null : region.lDongRegnCd();
        String lDongSignguCd = region == null ? null : region.lDongSignguCd();
        String contentTypeId = ExploreConditionMapper.toTourContentTypeId(category).orElse(null);

        return tourService.search(lDongRegnCd, lDongSignguCd, contentTypeId, page).stream()
                .map(tour -> ExploreItem.fromTour(tour, DEFAULT_MATCH_STATUS))
                .toList();
    }

    /**
     * facility는 카테고리가 1:N으로 매핑될 수 있어서(예: CULTURE -> 박물관/미술관/문예회관), 매핑된 category3
     * 값마다 기존 FacilityService.search()를 그대로 반복 호출해서 합친다 - FacilityService/Repository는
     * 하나도 안 건드리고 어댑터 계층(여기)에서만 해결한다. 같은 시설이 여러 카테고리에 겹쳐 나올 일은 없지만
     * (category3는 시설당 하나) id 기준으로 한 번 더 방어적으로 중복 제거한다.
     */
    private List<ExploreItem> searchFacilityItems(String regionCode, String category, int page) {
        String regionKeyword = ExploreConditionMapper.toFacilityRegionKeyword(regionCode);
        List<String> categoryValues = ExploreConditionMapper.toFacilityCategory3Values(category);

        if (categoryValues.isEmpty()) {
            return facilityService.search(regionKeyword, null, page).stream()
                    .map(facility -> ExploreItem.fromFacility(facility, DEFAULT_MATCH_STATUS))
                    .toList();
        }

        List<ExploreItem> merged = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (String categoryValue : categoryValues) {
            for (FacilitySummaryResponse facility : facilityService.search(regionKeyword, categoryValue, page)) {
                if (seenIds.add(facility.id())) {
                    merged.add(ExploreItem.fromFacility(facility, DEFAULT_MATCH_STATUS));
                }
            }
        }
        return merged;
    }

    /**
     * 항목마다 TourAPI 상세(관광지는 3번 호출) + 규칙/AI 판정을 돌려야 해서 항목 하나하나가 비싸다.
     * 페이지당 최대 수십 건이라 어느 정도 병렬로 돌려서 벽시계 시간을 줄인다 - TourAPI 호출량 자체(요청 수)는
     * 줄지 않고, Gemini 무료 티어 분당 한도(5회)는 페이지 하나에 판정이 몇 건만 필요해도 쉽게 넘을 수 있다.
     * 그래서 항목 하나의 판정 실패(레이트리밋/타임아웃 등 무엇이든)가 전체 응답을 깨뜨리면 안 된다 -
     * 실패한 항목은 기본값("확인필요")으로 조용히 대체하고 나머지는 정상 반환한다.
     */
    private List<ExploreItem> computeMatchStatuses(List<ExploreItem> items, Pet pet) {
        List<CompletableFuture<ExploreItem>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> computeOne(item, pet), matchExecutor))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private ExploreItem computeOne(ExploreItem item, Pet pet) {
        try {
            MatchResponse match = "tourapi".equals(item.source())
                    ? matchingService.matchTourForPet(pet, item.id())
                    : matchingService.matchFacilityForPet(pet, item.id());
            return item.withMatchStatus(match.status());
        } catch (Exception e) {
            log.warn("항목 매칭 계산 실패 - 확인필요로 대체함: source={}, id={}, error={}",
                    item.source(), item.id(), e.getMessage());
            return item; // 이미 DEFAULT_MATCH_STATUS로 채워져 있는 상태 그대로 반환
        }
    }

    private List<ExploreItem> mergeTourApiFirst(List<ExploreItem> tourItems, List<ExploreItem> facilityItems) {
        List<ExploreItem> merged = new ArrayList<>(tourItems);
        for (ExploreItem facilityItem : facilityItems) {
            boolean isDuplicate = tourItems.stream().anyMatch(tourItem -> isSamePlace(tourItem, facilityItem));
            if (!isDuplicate) {
                merged.add(facilityItem);
            }
        }
        return merged;
    }

    /**
     * 두 소스 다 좌표가 있으면 좌표 근접도로 판단한다 (주소 표기 방식이 소스마다 달라 주소 비교보다 신뢰도가
     * 높음). 좌표가 없는 항목이 있으면 제목 정규화(dedupKey) 비교로 폴백한다.
     */
    private boolean isSamePlace(ExploreItem a, ExploreItem b) {
        if (a.lat() != null && a.lng() != null && b.lat() != null && b.lng() != null) {
            return distanceMeters(a.lat(), a.lng(), b.lat(), b.lng()) <= DEDUP_DISTANCE_METERS;
        }
        return a.dedupKey().equals(b.dedupKey());
    }

    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusMeters = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMeters * c;
    }
}
