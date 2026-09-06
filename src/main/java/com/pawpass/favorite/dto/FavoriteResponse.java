package com.pawpass.favorite.dto;

import com.pawpass.favorite.domain.Favorite;
import com.pawpass.global.util.DataSource;

import java.time.LocalDateTime;

public record FavoriteResponse(
        Long id,
        DataSource source,
        String contentId,
        LocalDateTime createdAt
) {
    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(), favorite.getSource(), favorite.getContentId(), favorite.getCreatedAt()
        );
    }
}
