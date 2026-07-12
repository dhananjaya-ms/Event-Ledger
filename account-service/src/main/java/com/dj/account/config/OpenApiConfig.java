package com.dj.account.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Account Service API",
                version = "0.0.1",
                description = "Manages account balances and transactions. Used by the Gateway service.",
                contact = @Contact(name = "Dev", email = "dev@example.com"),
                license = @License(name = "MIT")
        )
)
public class OpenApiConfig {
}
