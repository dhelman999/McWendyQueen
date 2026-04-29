package com.mcwendyqueen.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

/**
 * Specialization for Spring HTTP validation exceptions that expose both
 * BindingResult details and HTTP status metadata.
 *
 * Extends BindExceptionMessage to reuse field-error extraction while
 * overriding status/message behavior for request validation failures.
 */
public class SpringBindingExceptionMessage extends BindExceptionMessage {
    public SpringBindingExceptionMessage(Exception ex) {
        super(ex);
    }

    @Override
    public void setMessage() {
        message = "Request validation failed. Check the errors field for details.";
    }

    /**
     * Derives status code from Spring exception status metadata.
     */
    @Override
    public void initStatus() {
        if (exception instanceof MethodArgumentNotValidException ex) {
            code = ex.getStatusCode().value();
        }
        else if (exception instanceof WebExchangeBindException ex) {
            code = ex.getStatusCode().value();
        }

        status = HttpStatus.valueOf(code).getReasonPhrase();
    }
}
