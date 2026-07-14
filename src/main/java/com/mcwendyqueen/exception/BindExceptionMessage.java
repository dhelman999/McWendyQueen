package com.mcwendyqueen.exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.WebExchangeBindException;

/**
 * Handles Spring binding failures where field-level binding metadata is
 * available through BindingResult (for MVC BindException and WebFlux
 * WebExchangeBindException).
 */
public class BindExceptionMessage extends BaseExceptionMessage {

    public BindExceptionMessage(Exception ex) {
        super(ex);
    }

    @Override
    public void setMessage() {
        this.message = "Request binding failed. Check the errors field for details.";
    }

    @Override
    public void initStatus() {
        this.code = HttpStatus.BAD_REQUEST.value();
        this.status = HttpStatus.BAD_REQUEST.getReasonPhrase();
    }

    /**
     * Extracts per-field validation details into a serializable list:
     * field name, human-readable message, and rejected value.
     */
    @Override
    public Object getErrors() {
        List<Map<String, String>> errorDetails = new ArrayList<>();
        BindingResult result;

        if (this.exception instanceof BindException be) {
            result = be.getBindingResult();
        }
        else if (this.exception instanceof WebExchangeBindException we) {
            result = we.getBindingResult();
        }
        else {
            return errorDetails;
        }

        // Extract details from each field error
        errorDetails = result.getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> details = new HashMap<>();
                    details.put("field", error.getField());           // e.g., "email"
                    details.put("message", error.getDefaultMessage()); // e.g., "must be a valid email"
                    details.put("rejectedValue", String.valueOf(error.getRejectedValue())); // The bad input

                    return details;
                })
                .toList();

        return errorDetails;
    }
}
