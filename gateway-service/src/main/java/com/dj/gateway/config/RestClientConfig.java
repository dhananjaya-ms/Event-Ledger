package com.dj.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient bean
 * 
 * RestClient is a synchronous HTTP client that provides a simpler API compared to WebClient
 * for blocking operations. It's the recommended replacement for RestTemplate.
 */
@Configuration
public class RestClientConfig {

    @Value("${account-service.url:http://localhost:8080}")
    private String accountServiceUrl;

    /**
     * Creates a RestClient bean configured to communicate with the Account Service
     * 
     * @return configured RestClient instance
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(accountServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
