package com.pawpass.trip.repository;

import com.pawpass.trip.domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findAllByUserIdOrderByVisitedAtDesc(Long userId);
    void deleteAllByUserId(Long userId);
}
