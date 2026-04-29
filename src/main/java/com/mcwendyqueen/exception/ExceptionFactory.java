package com.mcwendyqueen.exception;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

@Component
/**
 * Factory/multiplexer that selects the most appropriate error payload class
 * for a thrown exception. This keeps GlobalExceptionHandler small while
 * delegating status/message/detail logic to polymorphic implementations.
 */
public class ExceptionFactory {
    public ExceptionFactory() {
    }

    /**
     * Chooses the concrete message model based on exception capabilities:
     * - Spring binding/validation exceptions -> SpringBindingExceptionMessage
     * - Generic BindException -> BindExceptionMessage
     * - HTTP-native ErrorResponse exceptions -> SpringHttpErrorMessage
     * - all others -> NonHttpExceptionMessage
     */
    public static BaseExceptionMessage createExceptionMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException || ex instanceof WebExchangeBindException) {
            return new SpringBindingExceptionMessage(ex);
        }
        else if(ex instanceof BindException) {
            return new BindExceptionMessage(ex);
        }
        else if(isHttpNative(ex)) {
            return new SpringHttpErrorMessage(ex);
        }

        return new NonHttpExceptionMessage(ex);
    }

    /**
     * HTTP-native Spring exceptions expose status metadata via ErrorResponse.
     */
    private static boolean isHttpNative(Exception ex) {
        return ex instanceof org.springframework.web.ErrorResponse;
    }
}
