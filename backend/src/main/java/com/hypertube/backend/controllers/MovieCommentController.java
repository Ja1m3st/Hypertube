package com.hypertube.backend.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.hypertube.backend.dto.CommentRequestDTO;
import com.hypertube.backend.dto.CommentResponseDTO;
import com.hypertube.backend.models.User;
import com.hypertube.backend.services.MovieCommentService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/movies/{movieId}/comments")
@RequiredArgsConstructor
public class MovieCommentController {
	
	private final MovieCommentService commentService;

	@GetMapping
	public ResponseEntity<List<CommentResponseDTO>> getComments(
		@PathVariable Long movieId
	) {
		return ResponseEntity.ok(commentService.getCommentsByMovie(movieId));
	}

	@PostMapping
	public ResponseEntity<CommentResponseDTO> addComment (
		@PathVariable Long movieId,
        @Valid @RequestBody CommentRequestDTO request,
        @AuthenticationPrincipal User user
	) {
		CommentResponseDTO newComment = commentService.addComment(user, movieId, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
	}
}
