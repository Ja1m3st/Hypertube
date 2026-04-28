package com.hypertube.backend.services;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypertube.backend.models.Movie;
import com.hypertube.backend.models.User;
import com.hypertube.backend.models.UserHistory;
import com.hypertube.backend.repositories.MovieRepository;
import com.hypertube.backend.repositories.UserHistoryRepository;
import com.hypertube.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserHistoryService {
    
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserHistoryRepository progressRepository;

	@Autowired
	private MovieRepository movieRepository;

	public void updateProgess(Long userId, Long movieId, String title, String year,  String posterPath, Integer percentage) {
		Movie movie = movieRepository.findByTitleAndYear(title, year)
			.orElseGet(() -> {
                Movie newMovie = new Movie();

                newMovie.setId(movieId);
                newMovie.setTitle(title);
				newMovie.setPosterPath(posterPath);
                newMovie.setYear(year);

                return movieRepository.save(newMovie); 
            });

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new RuntimeException("User not found"));

		UserHistory progress = progressRepository.findByUserIdAndMovieId(userId, movie.getId())
			.orElseGet(() -> {
				UserHistory new_progress = new UserHistory();
				new_progress.setUser(user);
				new_progress.setMovie(movie);
				return new_progress;
			});
		
		progress.setLastThreshold(percentage);
		progress.setLastWatchedAt(Instant.now());
		if (percentage >= 70) {
			progress.setCompleted(true);
		} else {
			progress.setCompleted(false);
		}

		progressRepository.save(progress);
	}
}
