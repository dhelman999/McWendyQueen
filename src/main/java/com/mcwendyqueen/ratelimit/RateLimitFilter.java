package com.mcwendyqueen.ratelimit;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcwendyqueen.exception.BaseExceptionMessage;
import com.mcwendyqueen.exception.ExceptionFactory;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import static com.mcwendyqueen.ApiConstants.ADMIN_PATH;
import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.ORDER_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

/**
 * Per-client rate limit for /api/v1. The client is request.getRemoteAddr() only; forwarded headers are
 * ignored. POST /orders spends from both ORDERS and GENERAL, admin spends nothing, the rest spends GENERAL.
 * Built in RateLimitConfig, not scanned, so @WebMvcTest slices without RateLimitProperties stay unaffected.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<String> V1_SEGMENTS = segments(API_BASE_PATH + V1_PATH);

    private static final String ADMIN_SEGMENT = segments(ADMIN_PATH).get(0);

    private static final String ORDERS_SEGMENT = segments(ORDER_PATH).get(0);

    private final BucketStore bucketStore;

    private final RateLimitProperties properties;

    private final ObjectMapper objectMapper;

    public RateLimitFilter(BucketStore bucketStore, RateLimitProperties properties, ObjectMapper objectMapper) {
        this.bucketStore = bucketStore;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RouteGroup[] groups = classify(request);

        if (groups.length == 0) {
            chain.doFilter(request, response);
            return;
        }

        String client = request.getRemoteAddr();
        RateLimitDecision decision = bucketStore.tryConsume(client, groups);

        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        // groups[0] is the request's own route group; an order denied by a drained general bucket still logs orders
        reject(response, client, groups[0], decision.retryAfterSeconds());
    }

    /**
     * Route groups to spend for this request, most specific first; empty means unlimited. Matches on the
     * path segments the dispatcher routes on (decoded, path parameters such as ";x=1" dropped), so
     * "/api/v1;v=1/orders" and "/api/v1/orders;x=1" both count as orders.
     */
    private static RouteGroup[] classify(HttpServletRequest request) {
        List<String> path = routedSegments(request);

        if (path.size() <= V1_SEGMENTS.size() || !path.subList(0, V1_SEGMENTS.size()).equals(V1_SEGMENTS)) {
            return new RouteGroup[] {RouteGroup.GENERAL};
        }

        List<String> rest = path.subList(V1_SEGMENTS.size(), path.size());

        if (rest.get(0).equals(ADMIN_SEGMENT)) {
            return new RouteGroup[0];
        }
        if (HttpMethod.POST.matches(request.getMethod()) && rest.equals(List.of(ORDERS_SEGMENT))) {
            return new RouteGroup[] {RouteGroup.ORDERS, RouteGroup.GENERAL};
        }

        return new RouteGroup[] {RouteGroup.GENERAL};
    }

    private static List<String> routedSegments(HttpServletRequest request) {
        return segments(RequestPath.parse(request.getRequestURI(), request.getContextPath()).pathWithinApplication());
    }

    private static List<String> segments(String path) {
        return segments(PathContainer.parsePath(path));
    }

    // Decoded segment values without path parameters; empty segments ("//") are dropped.
    private static List<String> segments(PathContainer path) {
        return path.elements().stream()
                .filter(PathContainer.PathSegment.class::isInstance)
                .map(element -> ((PathContainer.PathSegment)element).valueToMatch())
                .filter(segment -> !segment.isEmpty())
                .toList();
    }

    // Writes the error body here: an exception thrown from a filter never reaches GlobalExceptionHandler.
    private void reject(HttpServletResponse response, String client, RouteGroup group, long retryAfterSeconds)
            throws IOException {
        BaseExceptionMessage exceptionMessage =
                ExceptionFactory.createExceptionMessage(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS));

        // the console pattern does not print the MDC, so the request id goes in the message text
        log.warn("Rate limit exceeded: client={} group={} retryAfter={}s requestId={}",
                client, group.name().toLowerCase(), retryAfterSeconds, exceptionMessage.getRequestId());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), exceptionMessage.response(true));
    }
}
