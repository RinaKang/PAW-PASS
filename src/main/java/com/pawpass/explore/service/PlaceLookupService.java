package com.pawpass.explore.service;

import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.global.util.DataSource;
import com.pawpass.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * favorite/trip 목록에 title/addr을 조인할 때 쓰는 source(tourapi/kcisa)별 상세 조회.
 * 즐겨찾기/기록 시점 이후 원본이 삭제되거나(관광지 폐업 등) TourAPI가 응답에 실패할 수 있어,
 * 실패 시 예외를 던지지 않고 null을 반환해 목록 전체가 깨지지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class PlaceLookupService {

    private final TourService tourService;
    private final FacilityService facilityService;

    public PlaceDetail lookup(DataSource source, String contentId) {
        try {
            return switch (source) {
                case TOURAPI -> {
                    TourService.PlaceSummary summary = tourService.getSummary(contentId);
                    yield summary == null ? null : new PlaceDetail(summary.title(), summary.addr());
                }
                case KCISA -> {
                    FacilityDetailResponse detail = facilityService.getDetail(contentId);
                    yield new PlaceDetail(detail.title(), detail.addr());
                }
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record PlaceDetail(String title, String addr) {
    }
}
