package com.dj.account.metrics;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to track HTTP request metrics
 */
@Slf4j
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    private final RequestMetricsService metricsService;
    private static final String START_TIME_ATTR = "startTime";

    public MetricsInterceptor(RequestMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime == null) {
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        String endpoint = getEndpointName(request);
        int status = response.getStatus();
        
        // Record metrics based on response status
        boolean isError = status >= 400;
        if (isError) {
            metricsService.recordError(endpoint, duration);
        } else {
            metricsService.recordSuccess(endpoint, duration);
        }
    }

    /**
     * Extract endpoint name from request (method + path pattern)
     */
    private String getEndpointName(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        
        // Normalize path by removing IDs to group similar endpoints
        path = normalizePath(path);
        
        return method + " " + path;
    }

    /**
     * Normalize path by replacing numeric IDs with {id}
     */
    private String normalizePath(String path) {
        // Replace numeric IDs with {id} to group similar requests
        return path.replaceAll("/\\d+", "/{id}");
    }
}
