package com.hypertube.backend.repositories;

import com.hypertube.backend.models.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long>{
    // SELECT * FROM user_movie_progress WHERE user_id = ?;
    List<UserHistory> findByUserId(Long userId);

    // SELECT * FROM user_movie_progess WHERE user_id = ? AND movie_id = ?;
    Optional<UserHistory> findByUserIdAndMovieId(Long userId, Long movieId);
}
