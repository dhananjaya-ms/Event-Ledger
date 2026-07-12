package com.dj.gateway.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "traceparent";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(TRACE_HEADER);
        String traceId = null;
        if (incoming != null && !incoming.isBlank()) {
            String[] parts = incoming.split("-");
            if (parts.length >= 2) {
                traceId = parts[1];
            }
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader(TRACE_ID_HEADER);
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
