package com.pawpass.favorite.dto;

import com.pawpass.global.util.DataSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(
        @NotNull DataSource source,
        @NotBlank String contentId
) {
}
