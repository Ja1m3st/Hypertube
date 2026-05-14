package com.hypertube.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hypertube.backend.models.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    // SELECT * FROM movie WHERE title = ? AND year = ?
    Optional<Movie> findByTitleAndYear(String title, String year);

}
