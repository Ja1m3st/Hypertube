package com.hypertube.backend.services;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.hypertube.backend.repositories.UserFavoriteRepository;
import com.hypertube.backend.repositories.UserRepository;
import com.hypertube.backend.dto.FavoriteMovieDTO;
import com.hypertube.backend.models.Movie;
import com.hypertube.backend.models.User;
import com.hypertube.backend.models.UserFavorite;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFavoriteService {
	
	private final UserFavoriteRepository favoriteRepo;
	private final UserRepository userRepo;
	private final MovieService movieService;

	@Transactional(readOnly = true)
	public List<FavoriteMovieDTO> getUserFavorites(Long userId) {

		return favoriteRepo.findByUserId(userId).stream()
			.map(fav -> new FavoriteMovieDTO(
				fav.getMovie().getId(),
				fav.getMovie().getTitle(),
				fav.getMovie().getPosterPath(),
				fav.getCreatedAt()
			))
			.toList();
	}

	@Transactional
	public void addFavorite(Long userId, Long movieId) {

		try {
			User userRef = userRepo.getReferenceById(userId);
			Movie movie = movieService.getOrCreateMovie(movieId);

			UserFavorite newFavorite = new UserFavorite();
			newFavorite.setUser(userRef);
			newFavorite.setMovie(movie);
			favoriteRepo.save(newFavorite);

		} catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("The movie is already in your favorites.");
		}
	}

	@Transactional
	public void removeFavorite(Long userId, Long movieId) {
		int deleteRows = favoriteRepo.deleteByUserIdAndMovieId(userId, movieId);
		if (deleteRows == 0) {
            throw new IllegalArgumentException("The favorite doesn't exist or has already been eliminated.");
		}
	}

}
