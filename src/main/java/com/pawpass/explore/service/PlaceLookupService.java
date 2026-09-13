package com.pawpass.explore.service;

import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.global.util.DataSource;
import com.pawpass.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * favorite/trip 목록에 title/addr을 조인할 때 쓰는 source(tourapi/kcisa)별 상세 조회.
 * 즐겨찾기/기록 시점 이후 원본이 삭제되거나(관광지 폐업 등) TourAPI가 응답에 실패할 수 있어,
 * 실패 시 예외를 던지지 않고 null을 반환해 목록 전체가 깨지지 않게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceLookupService {

    private final TourService tourService;
    private final FacilityService facilityService;

    /**
     * 원래 IllegalArgumentException("존재하지 않는 장소")만 잡았는데, 그건 "원본이 삭제됨" 케이스만
     * 커버할 뿐 "TourAPI 호출 자체가 실패함"(레이트리밋/타임아웃 등 WebClientResponseException류)은
     * 못 잡아서 항목 하나 때문에 즐겨찾기/방문기록 목록 전체가 500 나는 실제 버그가 있었다(2026-09-13,
     * TourAPI 일일 요청 한도 초과로 발견 - ExploreService.computeOne()과 같은 이유로 넓게 잡는다).
     */
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
        } catch (Exception e) {
            log.warn("장소 상세 조회 실패 - 목록에서 title/addr 없이 표시됨: source={}, contentId={}, error={}",
                    source, contentId, e.getMessage());
            return null;
        }
    }

    public record PlaceDetail(String title, String addr) {
    }
}
