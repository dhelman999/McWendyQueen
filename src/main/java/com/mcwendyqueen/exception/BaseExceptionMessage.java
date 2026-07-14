package com.mcwendyqueen.exception;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import org.springframework.http.HttpStatus;

/**
 * Base error payload model used by the global exception handler.
 *
 * Subclasses override template methods to provide type-specific behavior:
 * - initStatus(): map exception type to HTTP code/reason.
 * - setMessage(): provide a client-facing summary message.
 * - getErrors(): return structured validation/binding details when applicable.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
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
        this.exception = ex;
        initStatus();
        this.type = ex.getClass().getSimpleName();
        setMessage();
        this.timeStamp = String.valueOf(System.currentTimeMillis());
        this.requestId = MDC.get(REQUEST_HEADER_ID);
    }

    public void initStatus() {
        this.code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.status = HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
    }

    /**
     * Default message for uncategorized failures.
     * Subclasses should override for domain-specific clarity.
     */
    public void setMessage() {
        this.message = "An unexpected error occurred.";
    }

    public HttpStatus getHttpStatus() {
        HttpStatus httpStatus = HttpStatus.resolve(getCode());

        if (httpStatus == null) {
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
        return response(false);
    }

    public Map<String, Object> response(boolean excludeErrors) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", getStatus());
        response.put("code", getCode());
        response.put("type", getType());
        response.put("message", getMessage());
        response.put("timeStamp", getTimeStamp());
        response.put("requestId", getRequestId());

        if (!excludeErrors) {
            response.put("errors", getErrors());
        }

        return response;
    }

    public boolean isInternalError() {
        HttpStatus status = getHttpStatus();

        return status == null || status.is5xxServerError();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean excludeErrors) {
        StringBuilder toString = new StringBuilder();

        for (Map.Entry<String, Object> entry : response(excludeErrors).entrySet()) {
            toString.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return toString.toString();
    }
}
