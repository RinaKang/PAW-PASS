package com.pawpass.trip.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.global.util.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * API 명세서 - RQ-07 방문 기록
 * POST /trips  { source, content_id, visited_at? }
 * GET  /trips
 */
@RestController
@RequiredArgsConstructor
public class TripController {

    public record TripRequest(DataSource source, String contentId, LocalDate visitedAt) {}

    @PostMapping("/trips")
    public ApiResponse<Object> add(@RequestBody TripRequest request) {
        // TODO: TripService 구현 - visitedAt null이면 오늘 날짜로 (Trip 엔티티 생성자에서 이미 처리됨)
        throw new UnsupportedOperationException("TODO: TripService 구현 필요");
    }

    @GetMapping("/trips")
    public ApiResponse<Object> list() {
        throw new UnsupportedOperationException("TODO: TripService 구현 필요");
    }
}
