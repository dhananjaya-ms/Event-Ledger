package com.dj.account.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AccountDetailsResponse {
    private String accountId;
    private BigDecimal balance;
    private List<TransactionResponse> recentTransactions;

    public AccountDetailsResponse() {
    }

    public AccountDetailsResponse(String accountId, BigDecimal balance, List<TransactionResponse> recentTransactions) {
        this.accountId = accountId;
        this.balance = balance;
        this.recentTransactions = recentTransactions;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }


    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public List<TransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<TransactionResponse> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}
