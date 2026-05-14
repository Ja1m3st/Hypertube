package com.hypertube.backend.services;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hypertube.backend.dto.UserDTO.ProfileUserResponseDTO;
import com.hypertube.backend.dto.PublicUserProfileDTO;
import com.hypertube.backend.dto.UserDTO;
import com.hypertube.backend.exceptions.ConflictException;
import com.hypertube.backend.exceptions.InvalidArgumentException;
import com.hypertube.backend.exceptions.ResourceNotFoundException;
import com.hypertube.backend.models.User;
import com.hypertube.backend.repositories.UserRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final JwtService jwtService;

	// ─────────────────────────────────────────────
	// PROFILE
	// ─────────────────────────────────────────────
	@Transactional
	public ProfileUserResponseDTO getProfile(User user) {

		User infoUser = userRepository.findById(user.getId())
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return new UserDTO.ProfileUserResponseDTO(
			infoUser.getUsername(),
			infoUser.getFirstName(),
			infoUser.getLastName(),
			infoUser.getEmail(),
			infoUser.getProfilePictureUrl());
	}

	// ─────────────────────────────────────────────
	// PUBLIC_PROFILE
	// ─────────────────────────────────────────────
	@Transactional
	public PublicUserProfileDTO getPublicProfile(String username) {

		User user = userRepository.findByUsername(username)
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return new PublicUserProfileDTO(
			user.getUsername(),
			user.getFirstName(),
			user.getLastName(),
			user.getProfilePictureUrl());
	}
	
	// ─────────────────────────────────────────────
	// REGISTER
	// ─────────────────────────────────────────────
	@Transactional
	public UserDTO.RegisterResponseDTO registerUser(UserDTO.RegisterRequestDTO request) {

		String username = request.username().trim();
		String email = request.email().trim();

		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("The email address is already in use.");
		}

		if (userRepository.existsByUsername(username)) {
			throw new ConflictException("This username is already in use. Please choose another one.");
		}
			
		User user = new User();

		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setEmail(email);
		user.setUsername(username);
		
		String HashedPassword = passwordEncoder.encode(request.password());
		user.setPassword(HashedPassword);

		userRepository.save(user);
	
		return new UserDTO.RegisterResponseDTO(true);
	}

	// ─────────────────────────────────────────────
	// LOGIN
	// ─────────────────────────────────────────────
	@Transactional
	public UserDTO.LoginResponseDTO loginUser(UserDTO.LoginRequestDTO request) {
		String email = request.email().trim();

		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> {
				return new BadCredentialsException("Incorrect email or password.");
			});

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BadCredentialsException("Incorrect email or password.");
		}

		return new UserDTO.LoginResponseDTO(jwtService.generateToken(user));
	}

	// ─────────────────────────────────────────────
	// RECOVER PASSWORD
	// ─────────────────────────────────────────────
	@Transactional
	public UserDTO.RecoverResponseDTO recoverPassword(UserDTO.RecoverRequestDTO request) {

		String email = request.email().trim();

		userRepository.findByEmail(email).ifPresentOrElse(
			user -> {
				String resetLink = "http://localhost:4000/reset-password?token="
					+ jwtService.generatePasswordResetToken(user);

				emailService.sendTextEmail(
					user.getEmail(),
					"Password recovery - Hypertube",
					buildRecoveryEmailBody(user.getUsername(), resetLink)
				);
			},
			() -> log.warn("Recovery attempt for unregistered email [{}]", email)
		);

		String responseMessage = "If your email address is registered in our system, you will receive a recovery link shortly.";

		return new UserDTO.RecoverResponseDTO(true, responseMessage);
	}

	// ─────────────────────────────────────────────
	// RESET PASSWORD
	// ─────────────────────────────────────────────
	@Transactional
	public UserDTO.ResetPasswordResponseDTO resetPassword(UserDTO.ResetPasswordRequestDTO request) {

		String token = request.token();


		Long userId = jwtService.extractUserId(token);

		if (jwtService.isTokenExpired(token)) {
			throw new InvalidArgumentException("The recovery link has expired. Please request a new one.");
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResourceNotFoundException("User not found."));


		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
		String message = "Your password has been successfully updated. You can now log in.";

		return new UserDTO.ResetPasswordResponseDTO(message);
	}

	// ─────────────────────────────────────────────
	// UPDATE USERNAME
	// ─────────────────────────────────────────────
	@Transactional
	public void updateUsername(Long userId, String username) {

		int updated = userRepository.updateUsernameById(userId, username);

		if (updated == 0) {
			throw new ResourceNotFoundException("User not found");
		}
	}

	// ─────────────────────────────────────────────
	// UPDATE EMAIL
	// ─────────────────────────────────────────────
	@Transactional
	public void updateEmail(String newEmail, Long userId) {

		int updated = userRepository.updateEmailById(userId, newEmail);

		if (updated == 0) {
			throw new ResourceNotFoundException("Email not found");
		}

	}

	// ─────────────────────────────────────────────
	// UPDATE PICTURE
	// ─────────────────────────────────────────────
	@Transactional
	public void updatePicture(String url, Long userId) {

		int updated = userRepository.updateProfilePictureUrlById(userId, url);

		if (updated == 0) {
			throw new ResourceNotFoundException("User not found");
		}

	}

	// ─────────────────────────────────────────────
	// UPDATE PASSWORD
	// ─────────────────────────────────────────────
	@Transactional
	public void updatePassword(UserDTO.UpdatePasswordRequestDTO body, User user) {

		User dbUser = userRepository.findById(user.getId())
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		boolean isOAuthUser = dbUser.getProvider() != null && !dbUser.getProvider().isBlank();

		if (isOAuthUser) {
			throw new InvalidArgumentException("OAuth users cannot change their password.");
		}
		
		if (!passwordEncoder.matches(body.oldPassword(), dbUser.getPassword())) {
			throw new InvalidArgumentException("The current password does not match.");
		}

		dbUser.setPassword(passwordEncoder.encode(body.newPassword()));
		userRepository.save(dbUser);
	}

	// ─────────────────────────────────────────────
	// PRIVATE HELPERS
	// ─────────────────────────────────────────────
	private String buildRecoveryEmailBody(String username, String resetLink) {
		return """
			Hello %s,

			You have requested to reset your password. Click the link below:
			%s

			This link will expire in 15 minutes.
			If this wasn't you, please ignore this email.
			""".formatted(username, resetLink);
	}

	@Transactional
    public void updateLanguage(String newLanguage, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setLanguage(newLanguage);
        userRepository.save(user);
    }

}
