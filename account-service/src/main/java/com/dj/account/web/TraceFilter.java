package com.dj.account.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Trace Filter for the Account Service.
 * 
 * This filter:
 * 1. Extracts trace ID from incoming requests (W3C traceparent or X-Trace-Id headers)
 * 2. Generates a new trace ID if none is present
 * 3. Stores the trace ID in MDC for use in structured logging
 * 4. Adds the trace ID to response headers
 * 5. Cleans up MDC after the request is processed
 */
@Component
public class TraceFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TraceFilter.class);

    public static final String TRACE_HEADER = "traceparent";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String traceId = extractTraceId(request);
        
        // Store in MDC for use in logging
        MDC.put(MDC_TRACE_ID, traceId);
        
        // Add to response header
        response.setHeader(TRACE_ID_HEADER, traceId);
        
        logger.debug("Processing request with trace ID: {}", traceId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    /**
     * Extract trace ID from request headers.
     * 
     * Priority:
     * 1. W3C traceparent header (extract trace-id part)
     * 2. X-Trace-Id header
     * 3. Generate new UUID
     */
    private String extractTraceId(HttpServletRequest request) {
        // Try W3C traceparent format first
        String traceparent = request.getHeader(TRACE_HEADER);
        if (traceparent != null && !traceparent.isBlank()) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2) {
                String traceId = parts[1];
                logger.debug("Extracted trace ID from W3C traceparent header: {}", traceId);
                return traceId;
            }
        }

        // Try X-Trace-Id header
        String xTraceId = request.getHeader(TRACE_ID_HEADER);
        if (xTraceId != null && !xTraceId.isBlank()) {
            logger.debug("Extracted trace ID from X-Trace-Id header: {}", xTraceId);
            return xTraceId;
        }

        // Generate new trace ID
        String generatedTraceId = UUID.randomUUID().toString();
        logger.debug("Generated new trace ID: {}", generatedTraceId);
        return generatedTraceId;
    }
}
