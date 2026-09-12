package com.pawpass.route.service;

import com.pawpass.global.util.GeoUtils;
import com.pawpass.route.dto.RouteSuggestRequest;
import com.pawpass.route.dto.RouteSuggestResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 선택한 지점들을 총 이동거리(직선거리 합)가 최소가 되는 순서로 정렬한다 - 출발지로 돌아오지 않는
 * 편도 동선(하루 코스) 기준. 지점 수가 RouteSuggestRequest에서 이미 최대 8개로 제한돼 있어
 * 순열 전수조사(8! = 40,320가지)로도 충분히 빠르다 - 근사 알고리즘 없이 항상 정확한 최적해를 구한다.
 */
@Service
public class RouteService {

    public RouteSuggestResponse suggest(RouteSuggestRequest request) {
        List<RouteSuggestRequest.Point> points = request.points();

        List<RouteSuggestRequest.Point> best = null;
        double bestDistanceMeters = Double.MAX_VALUE;
        for (List<RouteSuggestRequest.Point> candidate : permutations(points)) {
            double distance = totalDistanceMeters(candidate);
            if (distance < bestDistanceMeters) {
                bestDistanceMeters = distance;
                best = candidate;
            }
        }

        return toResponse(best, bestDistanceMeters);
    }

    private double totalDistanceMeters(List<RouteSuggestRequest.Point> order) {
        double total = 0;
        for (int i = 0; i < order.size() - 1; i++) {
            total += legDistanceMeters(order.get(i), order.get(i + 1));
        }
        return total;
    }

    private double legDistanceMeters(RouteSuggestRequest.Point from, RouteSuggestRequest.Point to) {
        return GeoUtils.distanceMeters(from.lat(), from.lng(), to.lat(), to.lng());
    }

    private RouteSuggestResponse toResponse(List<RouteSuggestRequest.Point> order, double totalDistanceMeters) {
        List<RouteSuggestResponse.Leg> legs = new ArrayList<>();
        for (int i = 0; i < order.size() - 1; i++) {
            RouteSuggestRequest.Point from = order.get(i);
            RouteSuggestRequest.Point to = order.get(i + 1);
            legs.add(new RouteSuggestResponse.Leg(from.id(), to.id(), toKm(legDistanceMeters(from, to))));
        }
        List<String> ids = order.stream().map(RouteSuggestRequest.Point::id).toList();
        return new RouteSuggestResponse(ids, toKm(totalDistanceMeters), legs);
    }

    private double toKm(double meters) {
        return Math.round(meters / 100.0) / 10.0; // km 소수점 1자리
    }

    /** N개 지점의 모든 순열(N!)을 생성한다. Heap's algorithm - 상한(8개)이 있어 재귀로도 충분히 빠르다. */
    private List<List<RouteSuggestRequest.Point>> permutations(List<RouteSuggestRequest.Point> points) {
        List<List<RouteSuggestRequest.Point>> result = new ArrayList<>();
        permute(new ArrayList<>(points), 0, result);
        return result;
    }

    private void permute(List<RouteSuggestRequest.Point> points, int start, List<List<RouteSuggestRequest.Point>> result) {
        if (start == points.size() - 1) {
            result.add(new ArrayList<>(points));
            return;
        }
        for (int i = start; i < points.size(); i++) {
            Collections.swap(points, start, i);
            permute(points, start + 1, result);
            Collections.swap(points, start, i);
        }
    }
}
