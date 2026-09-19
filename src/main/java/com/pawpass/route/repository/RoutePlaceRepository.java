package com.pawpass.route.repository;

import com.pawpass.route.domain.RoutePlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutePlaceRepository extends JpaRepository<RoutePlace, Long> {
    List<RoutePlace> findAllByUserIdOrderBySortOrderAsc(Long userId);
    void deleteAllByUserId(Long userId);
}
