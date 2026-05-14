package com.hypertube.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hypertube.backend.dto.FavoriteMovieDTO;
import com.hypertube.backend.models.User;
import com.hypertube.backend.services.UserFavoriteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me/favorites")
@RequiredArgsConstructor
public class UserFavoriteController {

	private final UserFavoriteService favoriteService;

	@GetMapping
	public ResponseEntity<List<FavoriteMovieDTO>> getMyFavoriteMovies(
		@AuthenticationPrincipal User user
	) {
		return ResponseEntity.ok(favoriteService.getUserFavorites(user.getId()));
	}

	@PostMapping("/{movieId}")
	public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal User user, @PathVariable Long movieId) {
		favoriteService.addFavorite(user.getId(), movieId);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@DeleteMapping("/{movieId}")
	public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal User user, @PathVariable Long movieId) {
		favoriteService.removeFavorite(user.getId(), movieId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
