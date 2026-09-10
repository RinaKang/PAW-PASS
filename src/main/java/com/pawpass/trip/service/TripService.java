package com.pawpass.trip.service;

import com.pawpass.explore.service.PlaceLookupService;
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
    private final PlaceLookupService placeLookupService;

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
        return tripRepository.findAllByUserIdOrderByVisitedAtDesc(userId).stream()
                .map(trip -> TripResponse.withDetail(
                        trip, placeLookupService.lookup(trip.getSource(), trip.getContentId())))
                .toList();
    }
}
