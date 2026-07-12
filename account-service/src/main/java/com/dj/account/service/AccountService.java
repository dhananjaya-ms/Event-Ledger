package com.dj.account.service;

import com.dj.account.dto.AccountDetailsResponse;
import com.dj.account.dto.TransactionRequest;
import com.dj.account.dto.TransactionResponse;

import java.util.List;

public interface AccountService {
    TransactionResponse applyTransaction(String accountId, TransactionRequest req);
    java.math.BigDecimal getBalance(String accountId);
    AccountDetailsResponse getAccountDetails(String accountId, int recentTxCount);
}
