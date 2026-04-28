package com.hypertube.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidArgumentException extends RuntimeException {
    
    public InvalidArgumentException(String message) {
        super(message);
    }
}