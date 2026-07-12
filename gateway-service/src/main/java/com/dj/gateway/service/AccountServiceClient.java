package com.dj.gateway.service;

import com.dj.gateway.dto.TransactionRequest;
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
     * 
     * @param accountId the account ID
     * @param req the transaction request
     * @param traceId the trace ID for request tracking
     */
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
}
