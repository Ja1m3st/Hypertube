package com.hypertube.backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hypertube.backend.dto.UserDTO;
import com.hypertube.backend.services.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO.RegisterResponseDTO> registerUser(
        @Valid @RequestBody UserDTO.RegisterRequestDTO request
    ) {
        UserDTO.RegisterResponseDTO response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO.LoginResponseDTO> loginUser(
        @Valid @RequestBody UserDTO.LoginRequestDTO request
    ) {
        return ResponseEntity.ok(userService.loginUser(request));
    }

    @PostMapping("/recover-password")
    public ResponseEntity<UserDTO.RecoverResponseDTO> recoverPassword(
        @Valid @RequestBody UserDTO.RecoverRequestDTO request
    ) {
        return ResponseEntity.ok(userService.recoverPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<UserDTO.ResetPasswordResponseDTO> resetPassword(
        @Valid @RequestBody UserDTO.ResetPasswordRequestDTO request
    ) {
        return ResponseEntity.ok(userService.resetPassword(request));
    }
}