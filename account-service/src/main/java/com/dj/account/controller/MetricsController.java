package com.dj.account.controller;

import com.dj.account.metrics.RequestMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller to expose custom metrics via REST endpoints
 */
@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final RequestMetricsService metricsService;

    public MetricsController(RequestMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * Get all metrics across all endpoints
     * Example: GET /metrics/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, RequestMetricsService.EndpointMetrics> allMetrics = metricsService.getAllMetrics();
        
        long totalRequests = allMetrics.values().stream()
                .mapToLong(RequestMetricsService.EndpointMetrics::getTotalRequests)
                .sum();
        
        long totalErrors = allMetrics.values().stream()
                .mapToLong(RequestMetricsService.EndpointMetrics::getErrorCount)
                .sum();
        
        double overallErrorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100 : 0;
        
        return ResponseEntity.ok(Map.of(
                "totalRequests", totalRequests,
                "totalErrors", totalErrors,
                "overallErrorRate", String.format("%.2f%%", overallErrorRate),
                "endpointMetrics", allMetrics
        ));
    }

    /**
     * Get metrics for a specific endpoint
     * Example: GET /metrics/endpoint?name=GET%20/accounts/{id}
     */
    @GetMapping("/endpoint")
    public ResponseEntity<?> getEndpointMetrics(String name) {
        RequestMetricsService.EndpointMetrics metrics = metricsService.getMetrics(name);
        
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
                "endpoint", metrics.getEndpoint(),
                "totalRequests", metrics.getTotalRequests(),
                "errorCount", metrics.getErrorCount(),
                "errorRate", String.format("%.2f%%", metrics.getErrorRate()),
                "avgLatency", String.format("%.2fms", metrics.getAvgLatency()),
                "minLatency", metrics.getMinLatency(),
                "maxLatency", metrics.getMaxLatency()
        ));
    }

    /**
     * Get all tracked endpoints
     * Example: GET /metrics/endpoints
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getAllEndpoints() {
        Map<String, RequestMetricsService.EndpointMetrics> allMetrics = metricsService.getAllMetrics();
        return ResponseEntity.ok(Map.of(
                "count", allMetrics.size(),
                "endpoints", allMetrics.keySet()
        ));
    }
}
