package com.dj.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration for API Documentation
 * 
 * Provides comprehensive API documentation including:
 * - API information and metadata
 * - Server configuration for development and production
 * - Security schemes for authentication
 * - Custom headers for request tracking
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Gateway Service API",
                version = "0.0.1",
                description = "Entry point for all client requests. Receives transaction events, validates input, enforces idempotency, stores event records, and calls the Account Service to apply transactions.",
                contact = @Contact(
                        name = "Development Team",
                        email = "dev@example.com",
                        url = "https://github.com/your-org/event-ledger"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8081",
                        description = "Local Gateway Service Development Environment"
                ),
                @Server(
                        url = "http://localhost:8080",
                        description = "Account Service (proxied through Gateway)"
                ),
                @Server(
                        url = "https://api.example.com/gateway",
                        description = "Production Gateway Service"
                )
        }
)
@SecurityScheme(
        name = "X-Trace-Id",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        description = "Unique trace ID for request tracking and correlation across services"
)
public class OpenApiConfig {
    
    /**
     * Gateway Service API Documentation
     * 
     * This configuration provides:
     * - Comprehensive API documentation via Swagger UI (accessible at /swagger-ui.html)
     * - OpenAPI specification endpoint at /v3/api-docs
     * - Detailed request/response schemas
     * - Server configuration for multiple environments
     * - Security scheme definition for X-Trace-Id header
     * 
     * Access Swagger UI at: http://localhost:8081/swagger-ui.html
     * Access OpenAPI JSON at: http://localhost:8081/v3/api-docs
     */
}
