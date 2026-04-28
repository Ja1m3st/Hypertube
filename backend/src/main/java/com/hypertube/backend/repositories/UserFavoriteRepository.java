package com.hypertube.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hypertube.backend.models.UserFavorite;


@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long>{

    List<UserFavorite> findByUserId(Long userId);

    Optional<UserFavorite> findByUserIdAndMovieId(Long userId, Long movieId);

    @Modifying
    @Query("DELETE FROM UserFavorite f WHERE f.user.id = :userId AND f.movie.id = :movieId")
    int deleteByUserIdAndMovieId(@Param("userId") Long userId, @Param("movieId") Long movieId);
}
