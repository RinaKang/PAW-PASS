package com.pawpass.trip.service;

import com.pawpass.trip.domain.Trip;
import com.pawpass.trip.dto.TripRequest;
import com.pawpass.trip.dto.TripResponse;
import com.pawpass.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;

    @Transactional
    public TripResponse add(Long userId, TripRequest request) {
        Trip trip = Trip.builder()
                .userId(userId)
                .source(request.source())
                .contentId(request.contentId())
                .visitedAt(request.visitedAt())
                .build();
        return TripResponse.from(tripRepository.save(trip));
    }

    public List<TripResponse> findAllByUser(Long userId) {
        // TODO: source별로 tour(실시간) / facility(DB) 상세정보 조인해서 반환 (explore 도메인 연동 필요)
        return tripRepository.findAllByUserIdOrderByVisitedAtDesc(userId).stream()
                .map(TripResponse::from)
                .toList();
    }
}
