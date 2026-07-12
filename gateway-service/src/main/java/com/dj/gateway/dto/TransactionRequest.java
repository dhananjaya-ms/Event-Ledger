package com.dj.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "TransactionRequest", description = "Request payload for applying a transaction to an account")
public class TransactionRequest {
    @Schema(description = "Transaction amount", example = "100.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Transaction description", example = "Payment for invoice INV-123")
    private String description;

    public TransactionRequest() {}
    public TransactionRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
