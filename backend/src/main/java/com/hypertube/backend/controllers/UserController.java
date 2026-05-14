package com.hypertube.backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hypertube.backend.dto.PublicUserProfileDTO;
import com.hypertube.backend.dto.UserDTO.ProfileUserResponseDTO;
import com.hypertube.backend.dto.UserDTO.UpdateEmailRequestDTO;
import com.hypertube.backend.dto.UserDTO.UpdatePasswordRequestDTO;
import com.hypertube.backend.dto.UserDTO.UpdatePictureRequestDTO;
import com.hypertube.backend.dto.UserDTO.UpdateUsernameRequestDTO;
import com.hypertube.backend.dto.UserDTO.UpdateLanguageRequestDTO;
import com.hypertube.backend.dto.FavoriteMovieDTO;
import com.hypertube.backend.models.User;
import com.hypertube.backend.repositories.UserRepository;
import com.hypertube.backend.services.UserFavoriteService;
import com.hypertube.backend.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserRepository userRepo;
	private final UserService userService;
	private final UserFavoriteService favoriteService;

	@GetMapping("/me")
	public ResponseEntity<ProfileUserResponseDTO> getMyProfile(
		@AuthenticationPrincipal User user)
		{
		ProfileUserResponseDTO profileUser = userService.getProfile(user);
		return ResponseEntity.ok(profileUser);
	}

	@GetMapping("/{username}/public")
	public ResponseEntity<PublicUserProfileDTO> getPublicProfile(
		@PathVariable String username
		) {
		PublicUserProfileDTO publicProfile = userService.getPublicProfile(username);
		return ResponseEntity.ok(publicProfile);
	}

	@GetMapping("/{username}/favorites")
	public ResponseEntity<List<FavoriteMovieDTO>> getPublicUserFavorites(@PathVariable String username) {

		User user = userRepo.findByUsername(username)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		List<FavoriteMovieDTO> favorites = favoriteService.getUserFavorites(user.getId());

		return ResponseEntity.ok(favorites);
	}

	@PatchMapping("/me/username")
	public ResponseEntity<Void> updateUsername(
		@AuthenticationPrincipal User user,
		@Valid @RequestBody UpdateUsernameRequestDTO request)
		{
		userService.updateUsername(user.getId(), request.username());
		return ResponseEntity.noContent().build(); 
	}

	@PatchMapping("/me/email")
	public ResponseEntity<Void> updateEmail(
		@AuthenticationPrincipal User user,
		@Valid @RequestBody UpdateEmailRequestDTO request)
		{
		userService.updateEmail(request.email(), user.getId());
		return ResponseEntity.noContent().build(); 
	}

	@PatchMapping("/me/picture")
	public ResponseEntity<Void> updatePicture(
		@AuthenticationPrincipal User user,
		@Valid @RequestBody UpdatePictureRequestDTO request)
		{
		userService.updatePicture(request.picture(), user.getId());
		return ResponseEntity.noContent().build(); 
	}

	@PatchMapping("/me/password")
	public ResponseEntity<Void> updatePassword(
		@AuthenticationPrincipal User user,
		@Valid @RequestBody UpdatePasswordRequestDTO request)
		{
		userService.updatePassword(request, user);
		return ResponseEntity.noContent().build(); 
	}

	@PatchMapping("/me/language")
    public ResponseEntity<Void> updateLanguage(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpdateLanguageRequestDTO request) 
    {
        userService.updateLanguage(request.language(), user.getId());
        return ResponseEntity.noContent().build(); 
    }

}
