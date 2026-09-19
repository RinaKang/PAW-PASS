package com.pawpass.explore.service;

import com.pawpass.explore.dto.ExploreItem;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.global.util.GeoUtils;
import com.pawpass.matching.dto.MatchResponse;
import com.pawpass.matching.service.MatchingService;
import com.pawpass.pet.domain.Pet;
import com.pawpass.tour.dto.TourSummaryResponse;
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
import java.util.function.Supplier;

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
     * Gemini 무료 티어가 분당 5회로 요청을 제한한다. 동시 실행 수를 올린다고 분당 총량 제한 자체가
     * 없어지진 않아서 AI가 필요한 항목은 여전히 그 한도에 걸리지만, 실패해도 "확인필요"로 안전하게
     * 대체하는 폴백이 이미 있어서(computeOne()) 예전처럼 3으로 낮게 유지할 이유가 없다.
     * 규칙 기반만으로 끝나는 대다수 항목(실측상 90%+)은 병렬로 훨씬 빨리 끝나므로,
     * petId를 넘긴 /explore 체감 대기시간(40건 기준 최대 2분 실측, 2026-09-11)을 줄이려고 올림.
     */
    private static final int MATCH_CONCURRENCY = 8;

    /**
     * tour 항목 하나 매칭에 TourAPI만 3번(+규칙으로 못 걸러지면 Gemini까지) 필요해서 비용/시간 대부분이
     * 여기서 나온다 - facility는 DB 조회 한 번뿐이고 규칙 커버리지도 훨씬 높아서(실측 93%) 상한을 안 둔다.
     * petId를 넘긴 /explore가 체감상 너무 오래 걸린다는 실측(40건 기준 최대 2분, 2026-09-11) 이후,
     * 비용이 제일 큰 tour 쪽만 상한을 둬서 AI/외부 API 호출 자체를 줄이는 방향으로 잡음
     * (사용자 요청: "최대한 AI 안 쓰는 방향으로"). 상한을 넘긴 항목은 매칭을 아예 시도하지 않고
     * 기본값("확인필요")으로 둔다 - petId 있는 기본 목록은 어차피 가능/조건부만 보여주므로 시각적으로도
     * "판정했지만 애매함"과 구분이 안 되어 자연스럽다.
     */
    private static final int MAX_TOUR_ITEMS_TO_MATCH = 10;

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
     * petId를 안 넘기면 대표 반려동물(User.primaryPetId)로 대체한다(2026-09-13 추가) - 반려동물이 1마리뿐인
     * 사용자는 매번 petId를 명시하지 않아도 자동으로 개인화된다. 대표 반려동물도 없으면(반려동물 미등록,
     * 또는 2마리 이상인데 아직 하나를 안 골랐거나) 개인화 판정 없이 전부 "확인필요"로 내려가고, matchStatus
     * 기본 필터는 적용하지 않는다(적용하면 전부 걸러져서 빈 목록만 나옴 - 의미 없음). 개인화되면 항목마다
     * 실제 매칭(TourAPI 실시간 조회 + 규칙/AI 판정)을 돌려서 진짜 match_status를 채우고, matchStatus를
     * 명시하지 않은 "기본" 요청은 가능/조건부만 반환한다.
     *
     * keyword가 있으면(2026-09-14 추가, 동선 화면 장소 검색용) regionCode/category는 무시하고 이름/주소
     * 검색으로 완전히 갈아탄다 - 사용자가 특정 장소를 찾는 중이라 "가능/조건부만" 기본 필터를 적용하면
     * 원하는 장소가 안 보여서 오히려 못 찾는 역효과가 나므로, keyword 모드에서는 개인화 매칭은 그대로
     * 계산하되(정보 제공용) DEFAULT_VISIBLE_STATUSES 필터는 적용하지 않는다. matchStatus를 명시하면
     * (keyword와 함께 와도) 그 필터는 그대로 존중한다.
     *
     * showAll=true면(2026-09-19 추가, 프론트 목록 카드에 판정 뱃지를 전부 보여주고 싶다는 요청 - "가능만
     * 필터링해서 보여주는 게 아니라 불가/확인필요까지 전부 뱃지로 구분해서 보여주고 싶다") keyword 모드와
     * 똑같은 이유로 DEFAULT_VISIBLE_STATUSES 필터를 건너뛴다 - 개인화 매칭 계산 자체는 그대로 하되, 결과를
     * 가능/조건부로 솎아내지 않고 전부 반환한다. matchStatus를 명시하면 이때도 그 필터가 우선한다.
     *
     * petIds(2026-09-19 추가, 다견 AND 판정)를 넘기면 petId 대신 그 목록으로 개인화한다 - 선택된 반려동물
     * 전부가 함께 이용 가능해야 "가능"으로 뜬다(MatchingService.combine 참고). 둘 다 넘기면 petIds가 우선.
     */
    public List<ExploreItem> explore(Long userId, String regionCode, String category, String matchStatus, Long petId, int page) {
        return explore(userId, regionCode, category, matchStatus, petId, page, null, false);
    }

    public List<ExploreItem> explore(Long userId, String regionCode, String category, String matchStatus, Long petId, int page, String keyword) {
        return explore(userId, regionCode, category, matchStatus, petId, page, keyword, false);
    }

    public List<ExploreItem> explore(Long userId, String regionCode, String category, String matchStatus, Long petId,
                                      int page, String keyword, boolean showAll) {
        return explore(userId, regionCode, category, matchStatus, petId, page, keyword, showAll, List.of());
    }

    public List<ExploreItem> explore(Long userId, String regionCode, String category, String matchStatus, Long petId,
                                      int page, String keyword, boolean showAll, List<Long> petIds) {
        boolean keywordMode = keyword != null && !keyword.isBlank();

        List<ExploreItem> tourItems;
        List<ExploreItem> facilityItems;
        if (keywordMode) {
            tourItems = safely(() -> searchTourItemsByKeyword(keyword, page), "keyword=" + keyword);
            facilityItems = searchFacilityItemsByKeyword(keyword, page);
        } else {
            // HOSPITAL처럼 TourAPI에 대응 개념이 아예 없는 카테고리는 tour 조회 자체를 건너뛴다(불필요한
            // TourAPI 호출도 안 나감) - ExploreConditionMapper.CategoryMapping.facilityOnly 참고.
            tourItems = ExploreConditionMapper.isFacilityOnly(category) ? List.of()
                    : safely(() -> searchTourItems(regionCode, category, page), "regionCode=" + regionCode + ", category=" + category);
            facilityItems = searchFacilityItems(regionCode, category, page);
        }

        List<ExploreItem> merged = mergeTourApiFirst(tourItems, facilityItems);

        List<Long> effectivePetIds = (petIds != null && !petIds.isEmpty()) ? petIds
                : (petId == null ? List.of() : List.of(petId));
        List<Pet> pets = matchingService.resolveOptionalPets(userId, effectivePetIds);
        boolean personalized = !pets.isEmpty();
        if (personalized) {
            merged = computeMatchStatuses(merged, pets);
        }

        if (matchStatus != null && !matchStatus.isBlank()) {
            return merged.stream()
                    .filter(item -> matchStatus.equals(item.matchStatus()))
                    .toList();
        }
        if (personalized && !keywordMode && !showAll) {
            return merged.stream()
                    .filter(item -> DEFAULT_VISIBLE_STATUSES.contains(item.matchStatus()))
                    .toList();
        }
        return merged;
    }

    private List<ExploreItem> searchTourItemsByKeyword(String keyword, int page) {
        return tourService.searchByKeyword(keyword, page).stream()
                .map(tour -> ExploreItem.fromTour(tour, DEFAULT_MATCH_STATUS))
                .toList();
    }

    private List<ExploreItem> searchFacilityItemsByKeyword(String keyword, int page) {
        return facilityService.searchByKeyword(keyword, page).stream()
                .map(facility -> ExploreItem.fromFacility(facility, DEFAULT_MATCH_STATUS))
                .toList();
    }

    /**
     * 공통 지역명을 tour 쪽 규격(lDongRegnCd/lDongSignguCd)으로 바꿔서 조회한다.
     * 매핑 테이블에 없는 지역/카테고리는 ExploreConditionMapper가 필터 없음(Optional.empty)으로 돌려주므로
     * 자동으로 전체 조회에 가깝게 동작한다 - 값 하나 잘못 왔다고 빈 목록이 되진 않는다.
     * "광주전남"(2026-09-19 추가) 같은 통합 가상 지역은 toTourRegions()가 지역 2개를 돌려주는데, 그 경우
     * 이 메서드가 각각 조회해서 합친다 - 같은 관광지가 두 지역 조회 결과에 겹쳐 나올 일은 원래 없지만
     * (하나의 좌표는 법정동코드 하나에만 속함) contentId 기준으로 한 번 더 방어적으로 중복 제거한다.
     */
    private List<ExploreItem> searchTourItems(String regionCode, String category, int page) {
        List<ExploreConditionMapper.TourRegion> regions = ExploreConditionMapper.toTourRegions(regionCode);
        String contentTypeId = ExploreConditionMapper.toTourContentTypeId(category).orElse(null);
        // CAFE처럼 contentTypeId(39=음식점)만으로는 안 갈라지는 카테고리는 cat1/cat2/cat3까지 추가로 넘긴다
        // (그 외 카테고리는 전부 null이라 기존 동작과 동일) - ExploreConditionMapper 카테고리 주석 참고.
        String cat1 = ExploreConditionMapper.toTourCat1(category);
        String cat2 = ExploreConditionMapper.toTourCat2(category);
        String cat3 = ExploreConditionMapper.toTourCat3(category);
        // FOOD처럼 서버단 "제외" 필터가 없는 카테고리는, 응답을 받은 뒤 이 cat3와 일치하는 항목(카페로 명시
        // 태그된 것)만 걸러낸다 - TourAPI 자체가 제외 필터를 지원하지 않아서 클라이언트 쪽에서 처리한다.
        String excludedCat3 = ExploreConditionMapper.toExcludedTourCat3(category);
        // cat3 태그가 아예 없어서 위 필터로 못 거른 카페는 이름 키워드로 한 번 더 거른다(FOOD만 해당,
        // 2026-09-13 추가 - "최대한 식당만 뜨게" 요청에 대한 보강. ExploreConditionMapper 주석 참고).
        boolean excludeCafeLikeNames = ExploreConditionMapper.shouldExcludeCafeLikeNames(category);

        List<ExploreItem> merged = new ArrayList<>();
        Set<String> seenContentIds = new HashSet<>();
        for (ExploreConditionMapper.TourRegion region : regions) {
            for (TourSummaryResponse tour : tourService.search(
                    region.lDongRegnCd(), region.lDongSignguCd(), contentTypeId, cat1, cat2, cat3, page)) {
                if (!seenContentIds.add(tour.contentId())) {
                    continue;
                }
                if (excludedCat3 != null && excludedCat3.equals(tour.cat3())) {
                    continue;
                }
                if (excludeCafeLikeNames && ExploreConditionMapper.looksLikeCafeByName(tour.title())) {
                    continue;
                }
                merged.add(ExploreItem.fromTour(tour, DEFAULT_MATCH_STATUS));
            }
        }
        return merged;
    }

    /**
     * tour 목록 조회(TourAPI areaBasedList2/searchKeyword2) 실패가 facility(KCISA, 자체 DB) 결과까지
     * 통째로 끌고 내려가지 않게 한다(2026-09-19 추가) - 이 메서드가 없으면 tourItems 조회에서 예외가
     * 터지는 순간 바로 다음 줄인 facilityItems 조회는 아예 시도조차 못 하고 /explore 전체가 503이
     * 난다(TourAPI 일일 호출 한도 초과로 실사용 중 실제로 겪은 문제). 항목별 매칭 실패를 "확인필요"로
     * 조용히 넘기는 computeOne()과 같은 원칙을, 그보다 앞단인 목록 조회 자체에도 적용한다 - 단, 매칭과
     * 달리 항목 하나짜리 폴백이 아니라 tour 결과 전체를 빈 목록으로 대체한다(부분 성공이 불가능한 단일
     * 목록 호출이라).
     */
    private List<ExploreItem> safely(Supplier<List<ExploreItem>> tourSearch, String context) {
        try {
            return tourSearch.get();
        } catch (Exception e) {
            log.warn("tour 목록 조회 실패 - facility 결과만으로 진행함: {}, error={}", context, e.getMessage());
            return List.of();
        }
    }

    /**
     * facility는 카테고리가 1:N으로 매핑될 수 있어서(예: CULTURE -> 박물관/미술관/문예회관), 매핑된 category3
     * 값마다 기존 FacilityService.search()를 그대로 반복 호출해서 합친다 - FacilityService/Repository는
     * 하나도 안 건드리고 어댑터 계층(여기)에서만 해결한다. "광주전남"(2026-09-19 추가) 같은 통합 가상
     * 지역은 toFacilityRegionKeywords()가 지역 키워드 2개를 돌려줘서, 지역×카테고리 조합마다 반복 호출한다.
     * 같은 시설이 여러 조합에 겹쳐 나올 일은 없지만(주소 하나는 지역 키워드 하나에만, category3도 시설당
     * 하나) id 기준으로 한 번 더 방어적으로 중복 제거한다.
     */
    private List<ExploreItem> searchFacilityItems(String regionCode, String category, int page) {
        List<String> regionKeywords = ExploreConditionMapper.toFacilityRegionKeywords(regionCode);
        List<String> categoryValues = ExploreConditionMapper.toFacilityCategory3Values(category);

        List<ExploreItem> merged = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (String regionKeyword : regionKeywords) {
            if (categoryValues.isEmpty()) {
                for (FacilitySummaryResponse facility : facilityService.search(regionKeyword, null, page)) {
                    if (seenIds.add(facility.id())) {
                        merged.add(ExploreItem.fromFacility(facility, DEFAULT_MATCH_STATUS));
                    }
                }
                continue;
            }
            for (String categoryValue : categoryValues) {
                for (FacilitySummaryResponse facility : facilityService.search(regionKeyword, categoryValue, page)) {
                    if (seenIds.add(facility.id())) {
                        merged.add(ExploreItem.fromFacility(facility, DEFAULT_MATCH_STATUS));
                    }
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
     * tour 항목은 MAX_TOUR_ITEMS_TO_MATCH개까지만 실제로 매칭을 시도한다 - 원래 순서(tour 먼저)는
     * 그대로 유지하고, 상한을 넘긴 tour 항목만 매칭 자체를 건너뛴다(기본값 그대로).
     */
    private List<ExploreItem> computeMatchStatuses(List<ExploreItem> items, List<Pet> pets) {
        int[] remainingTourBudget = {MAX_TOUR_ITEMS_TO_MATCH};
        List<CompletableFuture<ExploreItem>> futures = items.stream()
                .map(item -> {
                    boolean isTour = "tourapi".equals(item.source());
                    boolean withinBudget = !isTour || remainingTourBudget[0]-- > 0;
                    if (!withinBudget) {
                        return CompletableFuture.completedFuture(item); // 기본값("확인필요") 그대로, 매칭 시도 안 함
                    }
                    return CompletableFuture.supplyAsync(() -> computeOne(item, pets), matchExecutor);
                })
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private ExploreItem computeOne(ExploreItem item, List<Pet> pets) {
        try {
            MatchResponse match = "tourapi".equals(item.source())
                    ? matchingService.matchTourForPetsLightweight(pets, item.id())
                    : matchingService.matchFacilityForPets(pets, item.id());
            return item.withMatch(match);
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
            return GeoUtils.distanceMeters(a.lat(), a.lng(), b.lat(), b.lng()) <= DEDUP_DISTANCE_METERS;
        }
        return a.dedupKey().equals(b.dedupKey());
    }
}
