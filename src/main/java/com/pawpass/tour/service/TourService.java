package com.pawpass.tour.service;

import com.pawpass.tour.client.TourApiClient;
import com.pawpass.tour.dto.TourDetailResponse;
import com.pawpass.tour.dto.TourSummaryResponse;
import com.pawpass.tour.dto.external.TourAreaBasedListResponse;
import com.pawpass.tour.dto.external.TourDetailCommonItem;
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
        TourAreaBasedListResponse response = tourApiClient.areaBasedList(
                PAGE_SIZE, page, ARRANGE_MODIFIED_DESC, category,
                regionCode, null, null, null, null
        );

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

        return TourDetailResponse.of(common, intro, pet);
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
