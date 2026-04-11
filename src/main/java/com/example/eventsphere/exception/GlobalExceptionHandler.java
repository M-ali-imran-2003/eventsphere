package com.example.eventsphere.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.warn("Business rule violation at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        // You can even add custom properties that aren't in the standard!
        problemDetail.setProperty("timestamp", java.time.Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("CRITICAL: Unhandled exception at {}", request.getRequestURI(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", java.time.Instant.now());

        return problemDetail;
    }
}