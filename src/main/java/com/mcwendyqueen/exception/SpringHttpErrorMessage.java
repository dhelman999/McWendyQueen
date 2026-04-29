package com.mcwendyqueen.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;

/**
 * Handles HTTP-native Spring exceptions that implement ErrorResponse.
 * These exceptions already carry an HTTP status code, so this class maps
 * status directly from the exception and applies a generic HTTP error message.
 */
public class SpringHttpErrorMessage extends BaseExceptionMessage {
    public SpringHttpErrorMessage(Exception ex) {
        super(ex);
    }

    @Override
    public void setMessage() {
        message = "Request failed due to an HTTP error.";
    }

    /**
     * Reads canonical HTTP status metadata from ErrorResponse.
     */
    @Override
    public void initStatus() {
        ErrorResponse errorResponseException = (ErrorResponse)exception;
        code = errorResponseException.getStatusCode().value();
        status = HttpStatus.valueOf(code).getReasonPhrase();
    }
}
