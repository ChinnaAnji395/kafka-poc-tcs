package com.kafka.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleSpecificExceptions(Exception ex, WebRequest request) {
        logger.error("Invalid request: {}", ex.getMessage(), ex);
        return ResponseEntity.status(400)
                .body("Invalid request: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex, WebRequest request) throws Exception {
        if (ex instanceof RuntimeException && ex.getMessage().contains("Service temporarily unavailable")) {
            throw ex; // Let circuit breaker exceptions propagate
        }
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(500)
                .body("An unexpected error occurred. Please try again later.");
    }
}