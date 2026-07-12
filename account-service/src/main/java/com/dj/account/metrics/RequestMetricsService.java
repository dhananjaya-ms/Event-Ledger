package com.dj.account.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service to track and manage request metrics including:
 * - Request count by endpoint
 * - Error rate by endpoint
 * - Latency histogram by endpoint
 */
@Slf4j
@Service
public class RequestMetricsService {

    private final MeterRegistry meterRegistry;
    private final Map<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();

    public RequestMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record a successful request
     */
    public void recordSuccess(String endpoint, long durationMs) {
        recordRequest(endpoint, durationMs, false);
    }

    /**
     * Record a failed request
     */
    public void recordError(String endpoint, long durationMs) {
        recordRequest(endpoint, durationMs, true);
    }

    private void recordRequest(String endpoint, long durationMs, boolean isError) {
        EndpointMetrics metrics = getOrCreateEndpointMetrics(endpoint);
        
        // Update counters
        metrics.totalRequests++;
        if (isError) {
            metrics.errorCount++;
        }
        metrics.totalLatency += durationMs;
        metrics.minLatency = Math.min(metrics.minLatency, durationMs);
        metrics.maxLatency = Math.max(metrics.maxLatency, durationMs);
        
        // Record to Micrometer - Counter
        Counter.builder("http.request.count")
                .tag("endpoint", endpoint)
                .tag("status", isError ? "error" : "success")
                .register(meterRegistry)
                .increment();

        // Record to Micrometer - Timer
        Timer.builder("http.request.duration")
                .tag("endpoint", endpoint)
                .tag("status", isError ? "error" : "success")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));

        // Log metrics
        logMetrics(endpoint, metrics, durationMs, isError);
    }

    private EndpointMetrics getOrCreateEndpointMetrics(String endpoint) {
        return endpointMetrics.computeIfAbsent(endpoint, k -> new EndpointMetrics(endpoint));
    }

    private void logMetrics(String endpoint, EndpointMetrics metrics, long durationMs, boolean isError) {
        double errorRate = metrics.totalRequests > 0 
            ? (double) metrics.errorCount / metrics.totalRequests * 100 
            : 0;
        
        log.info("Request Metrics - Endpoint: {}, Total Requests: {}, Errors: {}, Error Rate: {:.2f}%, Latency: {}ms",
                endpoint, metrics.totalRequests, metrics.errorCount, errorRate, durationMs);
    }

    /**
     * Get metrics for a specific endpoint
     */
    public EndpointMetrics getMetrics(String endpoint) {
        return endpointMetrics.get(endpoint);
    }

    /**
     * Get all endpoint metrics
     */
    public Map<String, EndpointMetrics> getAllMetrics() {
        return new ConcurrentHashMap<>(endpointMetrics);
    }

    /**
     * Inner class to hold metrics for a specific endpoint
     */
    public static class EndpointMetrics {
        private final String endpoint;
        private long totalRequests = 0;
        private long errorCount = 0;
        private long minLatency = Long.MAX_VALUE;
        private long maxLatency = 0;
        private long totalLatency = 0;

        public EndpointMetrics(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public long getTotalRequests() {
            return totalRequests;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public double getErrorRate() {
            return totalRequests > 0 ? (double) errorCount / totalRequests * 100 : 0;
        }

        public long getMinLatency() {
            return minLatency == Long.MAX_VALUE ? 0 : minLatency;
        }

        public long getMaxLatency() {
            return maxLatency;
        }

        public double getAvgLatency() {
            return totalRequests > 0 ? (double) totalLatency / totalRequests : 0;
        }

        @Override
        public String toString() {
            return "EndpointMetrics{" +
                    "endpoint='" + endpoint + '\'' +
                    ", totalRequests=" + totalRequests +
                    ", errorCount=" + errorCount +
                    ", errorRate=" + String.format("%.2f%%", getErrorRate()) +
                    ", avgLatency=" + String.format("%.2fms", getAvgLatency()) +
                    ", minLatency=" + getMinLatency() +
                    ", maxLatency=" + maxLatency +
                    '}';
        }
    }
}
