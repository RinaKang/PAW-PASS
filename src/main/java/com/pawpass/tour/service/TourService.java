package com.pawpass.tour.service;

import com.pawpass.tour.client.TourApiClient;
import com.pawpass.tour.dto.TourSummaryResponse;
import com.pawpass.tour.dto.external.TourAreaBasedListResponse;
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
}
