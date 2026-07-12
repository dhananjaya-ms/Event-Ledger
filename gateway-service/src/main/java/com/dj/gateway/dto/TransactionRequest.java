package com.dj.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "TransactionRequest", description = "Request payload for applying a transaction to an account")
public class TransactionRequest {
    @Schema(description = "Transaction amount", example = "100.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
    
    @Schema(description = "Transaction type i.e credit or debit", example = "CREDIT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type; // "CREDIT" or "DEBIT";
    
    @Schema(description = "Transaction amount currency i.e INR", example = "INR", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency; // "USD" or "INR";
    

    @Schema(description = "Transaction description", example = "Payment for invoice INV-123")
    private String description;

    public TransactionRequest() {}
    public TransactionRequest(BigDecimal amount,String type, String currency, String description) {
        this.amount = amount;
        this.type = type;
        this.currency = currency;
        this.description = description;
        
    }

    public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    
}
