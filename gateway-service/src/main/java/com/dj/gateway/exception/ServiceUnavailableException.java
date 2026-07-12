package com.dj.gateway.exception;

/**
 * Exception thrown when the Account Service circuit breaker is open
 * This means the Account Service has been repeatedly failing and is temporarily unavailable
 */
public class ServiceUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public ServiceUnavailableException(String serviceName) {
        super("Service temporarily unavailable: " + serviceName + ". The service is experiencing issues and has been temporarily disabled. Please retry after a few moments.");
    }

    public ServiceUnavailableException(String serviceName, String reason) {
        super("Service temporarily unavailable: " + serviceName + ". Reason: " + reason);
    }
}
