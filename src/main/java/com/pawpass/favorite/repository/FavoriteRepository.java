package com.pawpass.favorite.repository;

import com.pawpass.favorite.domain.Favorite;
import com.pawpass.global.util.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findAllByUserId(Long userId);
    Optional<Favorite> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndSourceAndContentId(Long userId, DataSource source, String contentId);
    void deleteAllByUserId(Long userId);
}
