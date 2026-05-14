package com.hypertube.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class MethodArgumentTypeMismatchException extends RuntimeException {
    
    public MethodArgumentTypeMismatchException(String message) {
        super(message);
    }
}