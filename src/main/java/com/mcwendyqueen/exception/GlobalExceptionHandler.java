package com.mcwendyqueen.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    public static final String EXCEPTION_LOGGER_NAME = "ExceptionLogger";

    public static final Logger exceptionLogger = LoggerFactory.getLogger(EXCEPTION_LOGGER_NAME);

    public GlobalExceptionHandler() {
    }

    // Global fallback handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandledExceptions(Exception ex) {
        BaseExceptionMessage exceptionMessage = ExceptionFactory.createExceptionMessage(ex);
        return new ResponseEntity<>(exceptionMessage.response(), exceptionMessage.getHttpStatus());
    }
}
