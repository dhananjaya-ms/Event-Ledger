package com.dj.account.dto;

import java.math.BigDecimal;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
public class TransactionRequest {
    private BigDecimal amount;
    
    private String type; // "credit" or "debit"
    
    private String currency; // "USD" or "INR"

    public TransactionRequest() {
    }

    public TransactionRequest(BigDecimal amount, String txnType, String currency) {
        this.amount = amount;
        this.type = txnType;
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

   }
