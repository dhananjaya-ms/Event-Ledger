package com.dj.gateway.service;

import com.dj.gateway.dto.TransactionRequest;
import com.dj.gateway.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);

    private final RestClient restClient;

    @Autowired
    public AccountServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Applies a transaction to an account via the Account Service
     * Protected by a circuit breaker that stops calling the service if it repeatedly fails
     * 
     * @param accountId the account ID
     * @param req the transaction request
     * @param traceId the trace ID for request tracking
     * @throws ServiceUnavailableException when the circuit breaker is open (service is temporarily unavailable)
     */
    @CircuitBreaker(name = "accountService", fallbackMethod = "applyTransactionFallback")
    public void applyTransaction(String accountId, TransactionRequest req, String traceId) {
        try {
            restClient.post()
                    .uri("/accounts/{accountId}/transactions", accountId)
                    .header("X-Trace-Id", traceId)
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
            
            log.debug("Transaction applied successfully for account: {}", accountId);
        } catch (HttpClientErrorException ex) {
            log.error("Account service client error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (HttpServerErrorException ex) {
            log.error("Account service server error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to apply transaction for account: {}", accountId, ex);
            throw new RuntimeException("Failed to apply transaction: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fallback method called when the circuit breaker is open
     * This method is invoked when the Account Service is repeatedly failing
     */
    public void applyTransactionFallback(String accountId, TransactionRequest req, String traceId, Exception ex) {
        log.error("Circuit breaker OPEN for Account Service - cannot apply transaction for account: {}. Error: {}", 
                  accountId, ex.getMessage());
        throw new ServiceUnavailableException("Account Service", 
                "The service is currently experiencing issues. Your transaction will be retried automatically.");
    }
}
