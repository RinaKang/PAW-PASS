package com.pawpass.trip.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.trip.dto.TripRequest;
import com.pawpass.trip.dto.TripResponse;
import com.pawpass.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 방문 기록
 * POST /trips  { source, content_id, visited_at? }
 * GET  /trips
 */
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ApiResponse<TripResponse> add(@AuthenticationPrincipal Long userId, @Valid @RequestBody TripRequest request) {
        return ApiResponse.success(tripService.add(userId, request));
    }

    @GetMapping
    public ApiResponse<List<TripResponse>> list(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(tripService.findAllByUser(userId));
    }
}
