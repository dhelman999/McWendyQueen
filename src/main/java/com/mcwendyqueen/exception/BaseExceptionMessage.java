package com.mcwendyqueen.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * Base error payload model used by the global exception handler.
 *
 * Subclasses override template methods to provide type-specific behavior:
 * - initStatus(): map exception type to HTTP code/reason.
 * - setMessage(): provide a client-facing summary message.
 * - getErrors(): return structured validation/binding details when applicable.
 */
public class BaseExceptionMessage {
    public static final String REQUEST_HEADER_ID = "requestId";

    protected Exception exception;

    protected String status;
    protected int code;
    protected String type;
    protected String message;
    protected String timeStamp;
    protected String requestId;
    protected Object errors;

    /**
     * Template-method construction flow:
     * 1) initialize status metadata
     * 2) set runtime exception type
     * 3) set message (overridable by subclasses)
     * 4) append common context fields
     */
    public BaseExceptionMessage(Exception ex) {
        exception = ex;
        initStatus();
        type = ex.getClass().getSimpleName();
        setMessage();
        timeStamp = String.valueOf(System.currentTimeMillis());
        requestId = MDC.get(REQUEST_HEADER_ID);
    }

    public void initStatus() {
        code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        status = HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
    }

    /**
     * Default message for uncategorized failures.
     * Subclasses should override for domain-specific clarity.
     */
    public void setMessage() {
        message = "An unexpected error occurred.";
    }

    public HttpStatus getHttpStatus() {
        HttpStatus httpStatus = HttpStatus.resolve(getCode());

        if(httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return httpStatus;
    }

    public Object getErrors() {
        return new HashMap<>();
    }

    /**
     * Builds the standardized API error shape returned by the handler.
     */
    public Map<String, Object> response() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", getStatus());
        response.put("code", getCode());
        response.put("type", getType());
        response.put("message", getMessage());
        response.put("timeStamp", getTimeStamp());
        response.put("requestId", getRequestId());
        response.put("errors", getErrors());

        return response;
    }
}
