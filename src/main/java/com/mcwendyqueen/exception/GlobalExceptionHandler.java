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

        // We want to account for 5xx internal errors separately to audit and improve upon.
        // These will also include the stack trace for debugging purposes
        if(exceptionMessage.isInternalError()) {
            exceptionLogger.error(exceptionMessage.toString(), ex);
        }
        // Non 5xx logs won't include detailed errors to avoid exposing sensitive data
        else {
            log.warn(exceptionMessage.toString(true));
        }

        return new ResponseEntity<>(exceptionMessage.response(), exceptionMessage.getHttpStatus());
    }
}
