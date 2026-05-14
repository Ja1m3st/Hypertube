package com.hypertube.backend.dto;

import java.time.LocalDateTime;

public record CommentResponseDTO(
    Long id,
    String text,
    LocalDateTime createdAt,
    String username,
    String firstName,
    String lastName,
    String avatar
) {}