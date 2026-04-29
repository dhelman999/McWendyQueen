package com.mcwendyqueen.exception;

import jakarta.validation.ValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Handles non-HTTP exceptions (typically application/database/runtime errors)
 * and maps them to API-facing status/message defaults.
 */
public class NonHttpExceptionMessage extends BaseExceptionMessage {
    public NonHttpExceptionMessage(Exception ex) {
        super(ex);
    }

    @Override
    public void setMessage() {
        if(exception instanceof ValidationException ||
           exception instanceof HttpMessageNotReadableException) {
            message = "Request body or parameters are invalid.";
        }
        else if(exception instanceof DataIntegrityViolationException ||
                exception instanceof OptimisticLockingFailureException) {
            message = "Request conflicts with current resource state.";
        }
        else if(exception instanceof EmptyResultDataAccessException) {
            message = "Requested resource was not found.";
        }
        else {
            message = "An unexpected server error occurred.";
        }
    }

    /**
     * Maps common non-HTTP exceptions into stable REST status codes.
     */
    @Override
    public void initStatus() {
        if(exception instanceof ValidationException ||
           exception instanceof HttpMessageNotReadableException) {
            code = HttpStatus.BAD_REQUEST.value();
        }
        else if(exception instanceof DataIntegrityViolationException ||
                exception instanceof OptimisticLockingFailureException) {
            code = HttpStatus.CONFLICT.value();
        }
        else if(exception instanceof EmptyResultDataAccessException) {
            code = HttpStatus.NOT_FOUND.value();
        }
        else {
            code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }

        status = HttpStatus.valueOf(code).getReasonPhrase();
    }
}
