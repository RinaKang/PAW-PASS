package com.pawpass.route.service;

import com.pawpass.route.dto.RouteSuggestRequest;
import com.pawpass.route.dto.RouteSuggestResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RouteServiceTest {

    private final RouteService routeService = new RouteService();

    // 같은 위도에 경도만 다르게(정확히 일직선) 배치 - 순서를 뒤섞어 넣어도 일직선상 순서(또는 역순)로만
    // 정렬돼야 한다. 일직선이라 다른 순서는 반드시 왔다갔다(backtrack)가 생겨 더 멀어짐 - 동률 없이 명확함.
    @Test
    void 총_이동거리가_최소가_되는_순서로_정렬한다() {
        RouteSuggestRequest.Point p1 = new RouteSuggestRequest.Point("A", 37.5, 127.0);
        RouteSuggestRequest.Point p2 = new RouteSuggestRequest.Point("B", 37.5, 127.1);
        RouteSuggestRequest.Point p3 = new RouteSuggestRequest.Point("C", 37.5, 127.5);

        // 입력 순서는 뒤섞어서 넣는다(C, A, B) - 결과가 입력 순서를 그대로 반환하는 게 아님을 확인
        RouteSuggestRequest request = new RouteSuggestRequest(List.of(p3, p1, p2));

        RouteSuggestResponse result = routeService.suggest(request);

        assertThat(result.order()).isIn(List.of("A", "B", "C"), List.of("C", "B", "A"));
    }

    @Test
    void legs와_총거리가_순서와_일치한다() {
        RouteSuggestRequest.Point a = new RouteSuggestRequest.Point("A", 37.5663, 126.9779);
        RouteSuggestRequest.Point b = new RouteSuggestRequest.Point("B", 37.5759, 126.9769);

        RouteSuggestResponse result = routeService.suggest(new RouteSuggestRequest(List.of(a, b)));

        assertThat(result.order()).containsExactly("A", "B");
        assertThat(result.legs()).hasSize(1);
        assertThat(result.legs().get(0).fromId()).isEqualTo("A");
        assertThat(result.legs().get(0).toId()).isEqualTo("B");
        assertThat(result.legs().get(0).distanceKm()).isEqualTo(result.totalDistanceKm());
        assertThat(result.totalDistanceKm()).isGreaterThan(0);
    }

    @Test
    void 여덟_개까지는_정상_계산된다() {
        List<RouteSuggestRequest.Point> points = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            points.add(new RouteSuggestRequest.Point("p" + i, 37.5 + i * 0.01, 127.0 + i * 0.01));
        }

        RouteSuggestResponse result = routeService.suggest(new RouteSuggestRequest(points));

        assertThat(result.order()).hasSize(8);
        assertThat(result.legs()).hasSize(7);
    }
}
