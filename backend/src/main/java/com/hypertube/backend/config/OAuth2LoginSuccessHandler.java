package com.hypertube.backend.config;

import com.hypertube.backend.models.User;
import com.hypertube.backend.repositories.UserRepository;
import com.hypertube.backend.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final UserRepository userRepository;
	private final JwtService jwtService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
		
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

		OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
		String provider = oauthToken.getAuthorizedClientRegistrationId();

		String email = "";
		String firstName = "";
		String lastName = "";
		String profilePicture = "";
		String username = "";

		if ("google".equals(provider)) {
			email = oAuth2User.getAttribute("email");
			firstName = oAuth2User.getAttribute("given_name");
			if (firstName == null) {
				firstName = oAuth2User.getAttribute("name"); 
			}
			lastName = oAuth2User.getAttribute("family_name");
			profilePicture = oAuth2User.getAttribute("picture");
		
			username = null;
		} else if ("fortytwo".equals(provider)) {
			email = oAuth2User.getAttribute("email");
			firstName = oAuth2User.getAttribute("first_name");
			lastName = oAuth2User.getAttribute("last_name");
			username = oAuth2User.getAttribute("login");
			
			Map<String, Object> imageObj = oAuth2User.getAttribute("image");
			if (imageObj != null && imageObj.containsKey("link")) {
				profilePicture = (String) imageObj.get("link");
			}
		}

		final String finalEmail = email;
		final String finalFirstName = firstName;
		final String finalLastName = lastName;
		final String finalProfilePicture = profilePicture;
		final String finalUsername = username;

		User user = userRepository.findByEmail(finalEmail).orElse(null);
		boolean needsUsernameSetup = false;

		if (user == null) {			
			user = new User();
			user.setEmail(finalEmail);
			user.setFirstName(finalFirstName != null ? finalFirstName : "User");
			user.setLastName(finalLastName != null ? finalLastName : "");
			user.setProfilePictureUrl(finalProfilePicture);
			user.setProvider(provider);
			user.setEmailVerified(true);
			if (finalUsername == null || finalUsername.isEmpty()) {
				user.setUsername("PENDING_" + java.util.UUID.randomUUID().toString().substring(0, 8));
				needsUsernameSetup = true;
			} else {
				user.setUsername(finalUsername);
			}

			userRepository.save(user);
		} else {
			boolean needsUpdate = false;
			if ((user.getProfilePictureUrl() == null || user.getProfilePictureUrl().isEmpty()) 
				&& finalProfilePicture != null && !finalProfilePicture.isEmpty()) {
				user.setProfilePictureUrl(finalProfilePicture);
				needsUpdate = true;
			}
			if (user.getUsername() == null || user.getUsername().isEmpty()) {
				user.setUsername(finalUsername);
				needsUpdate = true;
			}
			if (user.getUsername().startsWith("PENDING_")) {
				needsUsernameSetup = true;
			}
			if (needsUpdate) {
				userRepository.save(user);
			}
		}


		String jwtToken = jwtService.generateToken(user);
		String targetUrl = "http://localhost:4000/auth-callback?token=" + jwtToken;
		if (needsUsernameSetup) {
			targetUrl += "&setup=true";
		}
		
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}