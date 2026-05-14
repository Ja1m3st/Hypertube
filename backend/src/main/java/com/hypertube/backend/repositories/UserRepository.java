package com.hypertube.backend.repositories;

import org.springframework.stereotype.Repository;
import com.hypertube.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);
	
	boolean existsByUsername(String username);

	boolean existsByUsernameAndEmail(String username, String email);

	@Modifying
	@Query("UPDATE User u SET u.username = :newUsername WHERE u.id = :id")
	int updateUsernameById(@Param("id") Long id, @Param("newUsername") String newUsername);

	@Modifying
	@Query("UPDATE User u SET u.email = :newEmail WHERE u.id = :id")
	int updateEmailById(@Param("id") Long id, @Param("newEmail") String newEmail);

	@Modifying
	@Query("UPDATE User u SET u.profilePictureUrl = :newprofilePictureUrl WHERE u.id = :id")
	int updateProfilePictureUrlById(@Param("id") Long id, @Param("newprofilePictureUrl") String newprofilePictureUrl);
}
