package com.mcwendyqueen.exception;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String requestId = UUID.randomUUID().toString();
        // Store ID in MDC for the duration of this thread
        MDC.put("requestId", requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            // CRITICAL: Always clear MDC to prevent ID leakage to other requests
            MDC.clear();
        }
    }
}
