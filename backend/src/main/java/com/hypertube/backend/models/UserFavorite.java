package com.hypertube.backend.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(
	name = "user_favorite",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_user_favorite",
			columnNames = {"user_id", "movie_id"}
		)
	}
)
public class UserFavorite {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "movie_id", nullable = false)
	private Movie movie;

	@CreationTimestamp
	private LocalDateTime createdAt;

}
