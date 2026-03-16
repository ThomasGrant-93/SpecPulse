package com.specpulse.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds correlation IDs to MDC for distributed tracing
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Generate or extract trace IDs
            String traceId = request.getHeader("X-Trace-ID");
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }

            String spanId = UUID.randomUUID().toString().substring(0, 16);

            // Add to MDC
            MDC.put(TRACE_ID, traceId);
            MDC.put(SPAN_ID, spanId);
            MDC.put(REQUEST_ID, traceId + "-" + spanId);

            // Add to response headers
            response.setHeader("X-Trace-ID", traceId);
            response.setHeader("X-Span-ID", spanId);

            // Log request
            logRequest(request, traceId);

            // Continue filter chain
            filterChain.doFilter(request, response);

            // Log response
            logResponse(response, traceId);

        } finally {
            // Clear MDC
            MDC.clear();
        }
    }

    private void logRequest(HttpServletRequest request, String traceId) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingFilter.class);
        log.debug("HTTP Request: {} {} | TraceID: {} | IP: {}",
                request.getMethod(),
                request.getRequestURI(),
                traceId,
                getClientIp(request));
    }

    private void logResponse(HttpServletResponse response, String traceId) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingFilter.class);
        log.debug("HTTP Response: {} | TraceID: {}",
                response.getStatus(),
                traceId);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
