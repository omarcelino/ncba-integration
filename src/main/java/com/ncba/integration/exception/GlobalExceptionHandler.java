package com.ncba.integration.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ApiError(Instant timestamp, int status, String error,
                           String message, String Path, String correlationId) {}

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {

    }
}
