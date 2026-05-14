package com.hypertube.backend.controllers;

import com.hypertube.backend.exceptions.ConflictException;
import com.hypertube.backend.exceptions.InvalidArgumentException;
import com.hypertube.backend.exceptions.ResourceNotFoundException;
import com.hypertube.backend.exceptions.MethodArgumentTypeMismatchException;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    record ErrorResponse(int status, String message) {}
    record ValidationErrorResponse(int status, String message, List<String> errors) {}

    // 400 - Campos inválidos (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .toList();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ValidationErrorResponse(400, "Validation failed", errors));
    }

    // 400 - Parámetro de query faltante (?page, ?size, etc.)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Missing parameter: " + ex.getParameterName()));
    }

    // 400 - Tipo de parámetro incorrecto (/users/abc cuando espera Long)
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Invalid value for parameter: " + ex.getMessage()));
    }

    // 401 - Credenciales incorrectas (login fallido)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(401, "Invalid credentials"));
    }

    // 401 - Cuenta deshabilitada
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(401, "Account is disabled"));
    }

    // 401 - Cuenta bloqueada
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(401, "Account is locked"));
    }

    // 403 - Autenticado pero sin permisos
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(403, "Access denied"));
    }

    // 404 - Recurso no encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, ex.getMessage()));
    }

    // 409 - Conflicto: username o email duplicado (UNIQUE constraint)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(409, "Resource already exists"));
    }
	
	// 409 - Conflict (username, email duplicado con mensaje claro)
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
		return ResponseEntity
			.status(HttpStatus.CONFLICT)
			.body(new ErrorResponse(409, ex.getMessage()));
	}

    // 413 - Archivo demasiado grande
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ErrorResponse(413, "File size exceeds the maximum allowed limit"));
    }

    // 422 - Lógica de negocio inválida (password incorrecto, estado inválido, etc.)
    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(InvalidArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse(422, ex.getMessage()));
    }

    // 500 - Cualquier error no controlado (siempre al final)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "An unexpected error occurred. Please try again."));
    }
}