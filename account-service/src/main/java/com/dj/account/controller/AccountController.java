package com.dj.account.controller;

import com.dj.account.dto.AccountDetailsResponse;
import com.dj.account.dto.TransactionRequest;
import com.dj.account.dto.TransactionResponse;
import com.dj.account.service.AccountService;

import jakarta.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> postTransaction(
    		 @PathVariable
    	     @Pattern(regexp = "[0-9]+", message = "Account ID must contain only digits")
    		 String accountId, @RequestBody TransactionRequest req) {
        TransactionResponse resp = accountService.applyTransaction(accountId, req);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountId) {
        BigDecimal balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDetailsResponse> getAccount(@PathVariable String accountId, @RequestParam(defaultValue = "10") int recentTx) {
        AccountDetailsResponse resp = accountService.getAccountDetails(accountId, recentTx);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }
}
