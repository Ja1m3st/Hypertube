package com.hypertube.backend.services;

import com.hypertube.backend.models.User;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hypertube.backend.dto.CommentRequestDTO;
import com.hypertube.backend.dto.CommentResponseDTO;
import com.hypertube.backend.models.Movie;
import com.hypertube.backend.models.MovieComment;
import com.hypertube.backend.repositories.MovieCommentRepository;
import com.hypertube.backend.repositories.UserRepository;


import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieCommentService {

	private final MovieCommentRepository commentRepo;
	private final UserRepository userRepo;
	private final MovieService movieService;

	@Transactional
	public CommentResponseDTO addComment(User ghostUser, Long movieId, CommentRequestDTO request) {

		Movie movie = movieService.getOrCreateMovie(movieId);

		User user = userRepo.findById(ghostUser.getId())
            .orElseThrow(() -> new RuntimeException("User not found."));

		MovieComment comment = new MovieComment();
		comment.setText(request.text().trim());
		comment.setUser(user);
		comment.setMovie(movie);

		MovieComment savedComment = commentRepo.save(comment);

		
		return new CommentResponseDTO(
            savedComment.getId(),
            savedComment.getText(),
            savedComment.getCreatedAt(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getProfilePictureUrl()
        );
	}

	@Transactional(readOnly = true)
	public List<CommentResponseDTO> getCommentsByMovie(Long movieId) {
		return commentRepo.findByMovieIdOrderByCreatedAtDesc(movieId)
			.stream()
			.map(c -> new CommentResponseDTO(
				c.getId(),
				c.getText(),
				c.getCreatedAt(),
				c.getUser().getUsername(),
				c.getUser().getFirstName(),
				c.getUser().getLastName(),
				c.getUser().getProfilePictureUrl()
			))
			.collect(Collectors.toList());
	}
}
