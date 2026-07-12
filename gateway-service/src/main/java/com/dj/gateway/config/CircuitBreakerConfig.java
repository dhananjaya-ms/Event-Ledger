package com.dj.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j Circuit Breaker
 * Monitors calls to the Account Service and stops calling if it repeatedly fails
 */
//@Configuration
public class CircuitBreakerConfig {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    /**
     * Circuit breaker for Account Service calls
     * Configuration:
     * - Opens after 5 consecutive failures
     * - Waits 30 seconds before trying again (half-open state)
     * - Records HTTP errors (4xx, 5xx) and timeouts as failures
     */
    @Bean
    public CircuitBreaker accountServiceCircuitBreaker(MeterRegistry meterRegistry) {
    	io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github
    			.resilience4j.circuitbreaker
    			.CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .slowCallRateThreshold(50) // Also consider slow calls as failures
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // Calls longer than 5s are considered slow
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds before trying again
                .permittedNumberOfCallsInHalfOpenState(3) // Try 3 calls in half-open state
                .minimumNumberOfCalls(5) // Minimum calls before evaluating failure rate
                .recordException(ex -> shouldRecordException(ex)) // Record specific exceptions
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("accountService", config);

        // Register event consumer for logging
        circuitBreaker.getEventPublisher()
                .onSuccess(event -> log.debug("Circuit breaker success: {}", event))
                .onError(event -> log.warn("Circuit breaker error: {}", event))
                .onStateTransition(event -> log.info("Circuit breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState()))
                .onCallNotPermitted(event -> log.error("Call not permitted - circuit breaker is OPEN: {}", event));

        // Register metrics
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(
                CircuitBreakerRegistry.ofDefaults()
        ).bindTo(meterRegistry);

        return circuitBreaker;
    }

    /**
     * Determine whether an exception should be recorded as a failure
     */
    private static boolean shouldRecordException(Throwable ex) {
        String message = ex.getMessage();
        
        // Don't record client errors (4xx) as failures for circuit breaker
        // These are application errors, not service unavailability
        if (ex instanceof org.springframework.web.client.HttpClientErrorException clientEx) {
            log.debug("Client error (4xx) - not recording as circuit breaker failure: {}", clientEx.getStatusCode());
            return false;
        }
        
        // Record server errors (5xx) and timeouts as failures
        if (ex instanceof org.springframework.web.client.HttpServerErrorException) {
            log.debug("Server error (5xx) - recording as circuit breaker failure");
            return true;
        }
        
        if (ex instanceof org.springframework.web.client.ResourceAccessException) {
            log.debug("Connection/timeout error - recording as circuit breaker failure");
            return true;
        }
        
        // Record other runtime exceptions
        log.debug("Recording exception as failure: {}", ex.getClass().getSimpleName());
        return true;
    }
}
