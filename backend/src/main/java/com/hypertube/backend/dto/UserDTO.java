package com.hypertube.backend.dto;


import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDTO {

    private UserDTO() {}

    public record LoginRequestDTO(
        @NotBlank(message = "Email is required.")
        @Email(message = "Must be a valid email format.")
        String email,

        @NotBlank(message = "Password is required.")
        String password
    ) {}

    public record LoginResponseDTO(
        String token
    ) {}

    public record RegisterRequestDTO(
        @NotBlank(message = "First name is required.")
        String firstName,

        @NotBlank(message = "Last name is required.")
        String lastName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Must be a valid email format.")
        String email,

        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 20, message = "The username must be between 3 and 20 characters.")
        String username,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters long.")
        String password
    ) {}

    public record RegisterResponseDTO(
        Boolean isRegistered
    ) {}

    public record RecoverRequestDTO(
        @NotBlank(message = "Email is required.")
        @Email(message = "Must be a valid email format.")
        String email
    ) {}

    public record RecoverResponseDTO(
        Boolean isChange,
        String message
    ) {}

    public record ResetPasswordRequestDTO(
        @NotBlank(message = "Token is required.")
        String token,

        @NotBlank(message = "New password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters long.")
        String newPassword
    ) {}

    public record ResetPasswordResponseDTO(
        String message
    ) {}

    public record ProfileUserRequestDTO(
        @NotBlank(message = "Token is required.")
        String token
    ) {}

    public record ProfileUserResponseDTO(
        String username,
        String firstName,
        String lastName,
        String email,
        String avatar
    ) {}

    public record UpdateUsernameRequestDTO(
        @NotBlank(message = "The username cannot be empty.")
        @Size(min = 3, max = 20, message = "The username must be between 3 and 20 characters.")
        String username
    ) {}

	public record UpdatePasswordRequestDTO(
		@NotBlank(message = "Old password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters long.")
        String oldPassword,

        @NotBlank(message = "New password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters long.")
        String newPassword
    ) {}

	public record UpdateEmailRequestDTO(
        @NotBlank(message = "Email is required.")
        @Email(message = "Must be a valid email format.")
        String email
    ) {}

    public record UpdatePictureRequestDTO(
        @NotBlank(message = "The picture URL cannot be empty.")
        @URL(message = "Must be a valid HTTP or HTTPS URL.")
        String picture
    ) {}

    public record UpdateLanguageRequestDTO(
        @NotBlank(message = "El idioma no puede estar vacío")
        String language
    ) {}
}