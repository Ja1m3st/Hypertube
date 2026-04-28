package com.hypertube.backend.dto;

import java.time.LocalDateTime;

public record FavoriteMovieDTO(
    Long movieId,
    String title,
    String posterPath,
    LocalDateTime addedAt
) {}
