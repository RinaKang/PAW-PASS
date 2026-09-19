package com.pawpass.route.service;

import com.pawpass.route.domain.RoutePlace;
import com.pawpass.route.dto.RoutePlaceRequest;
import com.pawpass.route.dto.RoutePlaceResponse;
import com.pawpass.route.repository.RoutePlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자가 편집 중인 동선 후보 장소 목록(장바구니 개념, 사용자당 1개) 저장/조회 - PC/모바일 등
 * 기기 간 동기화용(로컬 스토리지만으로는 기기 간 공유가 안 된다는 프론트 리포트로 2026-09-19 추가).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedRouteService {

    private final RoutePlaceRepository routePlaceRepository;

    public List<RoutePlaceResponse> findAll(Long userId) {
        return routePlaceRepository.findAllByUserIdOrderBySortOrderAsc(userId).stream()
                .map(RoutePlaceResponse::from)
                .toList();
    }

    /**
     * 추가/삭제/순서변경을 구분하지 않고 프론트가 보낸 배열을 "지금 이 상태 그대로"로 취급해 전체
     * 교체한다 - 프론트가 이미 로컬에서 add/remove/reorder를 다 처리한 최종 목록을 통째로 보내는 게
     * 자연스러운 흐름이라, 기존 값과 델타를 계산해서 부분 반영하는 것보다 서버/클라이언트 모두 단순하다.
     */
    @Transactional
    public List<RoutePlaceResponse> replaceAll(Long userId, List<RoutePlaceRequest> requests) {
        routePlaceRepository.deleteAllByUserId(userId);
        List<RoutePlace> places = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            RoutePlaceRequest request = requests.get(i);
            places.add(RoutePlace.builder()
                    .userId(userId)
                    .source(request.source())
                    .contentId(request.contentId())
                    .title(request.title())
                    .lat(request.lat())
                    .lng(request.lng())
                    .sortOrder(i)
                    .build());
        }
        return routePlaceRepository.saveAll(places).stream().map(RoutePlaceResponse::from).toList();
    }
}
