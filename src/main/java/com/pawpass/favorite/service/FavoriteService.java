package com.pawpass.favorite.service;

import com.pawpass.explore.service.PlaceLookupService;
import com.pawpass.favorite.domain.Favorite;
import com.pawpass.favorite.dto.FavoriteRequest;
import com.pawpass.favorite.dto.FavoriteResponse;
import com.pawpass.favorite.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PlaceLookupService placeLookupService;

    @Transactional
    public FavoriteResponse add(Long userId, FavoriteRequest request) {
        if (favoriteRepository.existsByUserIdAndSourceAndContentId(userId, request.source(), request.contentId())) {
            throw new IllegalArgumentException("이미 즐겨찾기한 장소입니다.");
        }
        Favorite favorite = Favorite.builder()
                .userId(userId)
                .source(request.source())
                .contentId(request.contentId())
                .build();
        return FavoriteResponse.from(favoriteRepository.save(favorite));
    }

    public List<FavoriteResponse> findAllByUser(Long userId) {
        return favoriteRepository.findAllByUserId(userId).stream()
                .map(favorite -> FavoriteResponse.withDetail(
                        favorite, placeLookupService.lookup(favorite.getSource(), favorite.getContentId())))
                .toList();
    }

    @Transactional
    public void remove(Long userId, Long id) {
        Favorite favorite = favoriteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 즐겨찾기입니다: " + id));
        favoriteRepository.delete(favorite);
    }
}
