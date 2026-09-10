package com.pawpass.favorite.dto;

import com.pawpass.explore.service.PlaceLookupService;
import com.pawpass.favorite.domain.Favorite;
import com.pawpass.global.util.DataSource;

import java.time.LocalDateTime;

public record FavoriteResponse(
        Long id,
        DataSource source,
        String contentId,
        String title,
        String addr,
        LocalDateTime createdAt
) {
    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(), favorite.getSource(), favorite.getContentId(), null, null, favorite.getCreatedAt()
        );
    }

    public static FavoriteResponse withDetail(Favorite favorite, PlaceLookupService.PlaceDetail detail) {
        return new FavoriteResponse(
                favorite.getId(), favorite.getSource(), favorite.getContentId(),
                detail == null ? null : detail.title(),
                detail == null ? null : detail.addr(),
                favorite.getCreatedAt()
        );
    }
}
