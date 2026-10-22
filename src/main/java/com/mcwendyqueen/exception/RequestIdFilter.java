package com.mcwendyqueen.exception;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.slf4j.MDC;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(RequestIdFilter.ORDER)
public class RequestIdFilter implements Filter {

    // Runs after Boot's CharacterEncodingFilter (HIGHEST_PRECEDENCE) and before every app
    // filter, so later filters such as the rate limiter see requestId in the MDC.
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String requestId = UUID.randomUUID().toString();

        // Store ID in MDC for the duration of this thread
        MDC.put("requestId", requestId);

        try {
            chain.doFilter(request, response);
        }
        finally {
            // CRITICAL: Always clear MDC to prevent ID leakage to other requests
            MDC.clear();
        }
    }
}
