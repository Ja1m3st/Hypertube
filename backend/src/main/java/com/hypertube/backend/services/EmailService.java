package com.hypertube.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String fromEmail;

	@Async
	public void sendTextEmail(String to, String subject, String body) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromEmail);
			message.setTo(to);
			message.setSubject(subject);
			message.setText(body);

			mailSender.send(message);

		} catch (Exception e) {
		
		}
	}
}
