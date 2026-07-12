package com.dj.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime timestamp;
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

	private String type; // "credit" or "debit"
    private String currency; // "USD" or "INR"

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, BigDecimal amount, LocalDateTime timestamp, String type, String currency) {
        this.id = id;
        this.amount = amount;
        this.timestamp = timestamp;
        this.type = type;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }


}
