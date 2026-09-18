package com.pawpass.tour.service;

import com.pawpass.tour.client.TourApiClient;
import com.pawpass.tour.dto.TourDetailResponse;
import com.pawpass.tour.dto.TourSummaryResponse;
import com.pawpass.tour.dto.external.TourAreaBasedListResponse;
import com.pawpass.tour.dto.external.TourDetailCommonItem;
import com.pawpass.tour.dto.external.TourDetailImageItem;
import com.pawpass.tour.dto.external.TourDetailIntroItem;
import com.pawpass.tour.dto.external.TourDetailPetTourItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * TourAPI 응답은 절대 DB에 저장하지 않음 - 매 요청마다 실시간 호출해서 그대로 매핑해 반환.
 */
@Service
@RequiredArgsConstructor
public class TourService {

    private static final int PAGE_SIZE = 20;
    private static final String ARRANGE_MODIFIED_DESC = "C"; // 수정일순(최신순)

    private final TourApiClient tourApiClient;

    public List<TourSummaryResponse> search(String regionCode, String category, int page) {
        return search(regionCode, null, category, page);
    }

    /**
     * /explore가 "강릉" 같은 시/군 단위 공통 지역값을 시/도(lDongRegnCd)+시/군구(lDongSignguCd) 조합으로
     * 변환해서 더 정밀하게 필터링할 때 쓴다 (explore/service/ExploreConditionMapper 참고).
     * GET /tours는 기존 3-args search()만 쓰므로 이 오버로드가 추가돼도 그 엔드포인트 동작은 그대로다.
     */
    public List<TourSummaryResponse> search(String lDongRegnCd, String lDongSignguCd, String category, int page) {
        return search(lDongRegnCd, lDongSignguCd, category, null, null, null, page);
    }

    /**
     * /explore의 카페(CAFE) 카테고리처럼, contentTypeId만으로는 안 갈라지는(39=음식점 안에 카페·일반식당이
     * 섞여 나옴) 세분류가 필요할 때 cat1/cat2/cat3(TourAPI 구 분류체계)까지 추가로 넘긴다
     * (explore/service/ExploreConditionMapper 참고). GET /tours는 이 오버로드를 쓰지 않으므로
     * 기존 엔드포인트 동작에는 영향 없다.
     */
    public List<TourSummaryResponse> search(String lDongRegnCd, String lDongSignguCd, String category,
                                              String cat1, String cat2, String cat3, int page) {
        TourAreaBasedListResponse response = tourApiClient.areaBasedList(
                PAGE_SIZE, page, ARRANGE_MODIFIED_DESC, category,
                lDongRegnCd, lDongSignguCd, null, null, null,
                cat1, cat2, cat3
        );

        var items = response.response().body().items();
        if (items == null || items.item() == null) {
            return Collections.emptyList();
        }
        return items.item().stream()
                .map(TourSummaryResponse::from)
                .toList();
    }

    /**
     * /explore의 keyword 검색(동선 화면 장소 검색 등)용 - regionCode/category 기반 search()와 달리
     * 지역 구분 없이 전국을 대상으로 이름 검색한다(TourApiClient.searchKeyword 참고).
     */
    public List<TourSummaryResponse> searchByKeyword(String keyword, int page) {
        TourAreaBasedListResponse response = tourApiClient.searchKeyword(PAGE_SIZE, page, ARRANGE_MODIFIED_DESC, keyword);

        var items = response.response().body().items();
        if (items == null || items.item() == null) {
            return Collections.emptyList();
        }
        return items.item().stream()
                .map(TourSummaryResponse::from)
                .toList();
    }

    public TourDetailResponse getDetail(String contentId) {
        TourDetailCommonItem common = tourApiClient.detailCommon(contentId);
        if (common == null) {
            throw new IllegalArgumentException("존재하지 않는 관광지입니다: contentId=" + contentId);
        }
        TourDetailIntroItem intro = tourApiClient.detailIntro(contentId, common.contentTypeId());
        TourDetailPetTourItem pet = tourApiClient.detailPetTour(contentId);
        List<TourDetailImageItem> images = tourApiClient.detailImage(contentId);

        return TourDetailResponse.of(common, intro, pet, images);
    }

    
    public TourDetailResponse.PetCondition getPetCondition(String contentId) {
        TourDetailPetTourItem pet = tourApiClient.detailPetTour(contentId);
        return TourDetailResponse.PetCondition.from(pet);
    }

    /**
     * favorite/trip 목록 조인처럼 title/addr만 필요한 곳에서 쓴다.
     * getDetail()과 달리 detailCommon2 한 번만 호출한다 (intro/petTour까지 부를 필요가 없어 API 호출을 아낌).
     */
    public PlaceSummary getSummary(String contentId) {
        TourDetailCommonItem common = tourApiClient.detailCommon(contentId);
        if (common == null) {
            return null;
        }
        String addr = (common.addr2() == null || common.addr2().isBlank())
                ? common.addr1()
                : common.addr1() + " " + common.addr2();
        return new PlaceSummary(common.title(), addr);
    }

    public record PlaceSummary(String title, String addr) {
    }
}
