package com.hypertube.backend.dto;


public record PublicUserProfileDTO(
    String username,
    String firstName,
    String lastName,
    String profilePictureUrl
) {}