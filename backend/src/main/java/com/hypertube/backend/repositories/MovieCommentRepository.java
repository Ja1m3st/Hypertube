package com.hypertube.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hypertube.backend.models.MovieComment;

public interface MovieCommentRepository extends JpaRepository<MovieComment, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<MovieComment> findByMovieIdOrderByCreatedAtDesc(Long movieId);

}
