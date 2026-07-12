package com.dj.account.controller;

import com.dj.account.dto.AccountDetailsResponse;
import com.dj.account.dto.TransactionRequest;
import com.dj.account.dto.TransactionResponse;
import com.dj.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AccountController Integration Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @DisplayName("POST Transaction - Valid request returns 201 Created")
    @Test
    void testPostTransaction_ValidRequest() throws Exception {
        String accountId = "123456";
        TransactionRequest req = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
        TransactionResponse resp = new TransactionResponse(1L, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD");

        when(accountService.applyTransaction(accountId, req)).thenReturn(resp);

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(100.0)))
                .andExpect(jsonPath("$.type", is("credit")))
                .andExpect(jsonPath("$.currency", is("USD")));

        verify(accountService).applyTransaction(accountId, req);
    }

    @DisplayName("POST Transaction - Invalid account ID (non-numeric) returns 400")
    @Test
    void testPostTransaction_InvalidAccountId() throws Exception {
        String accountId = "acc-123"; // Non-numeric, should be rejected
        TransactionRequest req = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");

        // The validation error should result in a 400 Bad Request
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        // Service should never be called with invalid account ID
        verify(accountService, never()).applyTransaction(anyString(), any());
    }

    @DisplayName("POST Transaction - Valid numeric account ID")
    @Test
    void testPostTransaction_ValidNumericAccountId() throws Exception {
        String accountId = "987654321";
        TransactionRequest req = new TransactionRequest(new BigDecimal("500.00"), "debit", "INR");
        TransactionResponse resp = new TransactionResponse(2L, new BigDecimal("500.00"), LocalDateTime.now(), "debit", "INR");

        when(accountService.applyTransaction(accountId, req)).thenReturn(resp);

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.amount", is(500.0)));

        verify(accountService).applyTransaction(accountId, req);
    }

    @DisplayName("POST Transaction - Missing request body returns 400")
    @Test
    void testPostTransaction_MissingRequestBody() throws Exception {
        String accountId = "123456";

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).applyTransaction(anyString(), any());
    }

    @DisplayName("POST Transaction - Multiple transactions same account")
    @Test
    void testPostTransaction_MultipleTransactionsSameAccount() throws Exception {
        String accountId = "555555";

        TransactionRequest req1 = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
        TransactionResponse resp1 = new TransactionResponse(1L, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD");

        TransactionRequest req2 = new TransactionRequest(new BigDecimal("50.00"), "debit", "USD");
        TransactionResponse resp2 = new TransactionResponse(2L, new BigDecimal("50.00"), LocalDateTime.now(), "debit", "USD");

        when(accountService.applyTransaction(accountId, req1)).thenReturn(resp1);
        when(accountService.applyTransaction(accountId, req2)).thenReturn(resp2);

        // First transaction
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Second transaction
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        verify(accountService, times(2)).applyTransaction(anyString(), any());
    }

    @DisplayName("GET Balance - Existing account")
    @Test
    void testGetBalance_ExistingAccount() throws Exception {
        String accountId = "123456";
        BigDecimal balance = new BigDecimal("2500.00");

        when(accountService.getBalance(accountId)).thenReturn(balance);

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("2500.00"));

        verify(accountService).getBalance(accountId);
    }

    @DisplayName("GET Balance - Non-existent account returns zero")
    @Test
    void testGetBalance_NonExistentAccount() throws Exception {
        String accountId = "999999";

        when(accountService.getBalance(accountId)).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        verify(accountService).getBalance(accountId);
    }

    @DisplayName("GET Balance - Negative balance")
    @Test
    void testGetBalance_NegativeBalance() throws Exception {
        String accountId = "111111";
        BigDecimal negativeBalance = new BigDecimal("-100.00");

        when(accountService.getBalance(accountId)).thenReturn(negativeBalance);

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("-100.00"));

        verify(accountService).getBalance(accountId);
    }

    @DisplayName("GET Account Details - With default recent transactions")
    @Test
    void testGetAccount_DefaultRecentTransactions() throws Exception {
        String accountId = "123456";
        
        List<TransactionResponse> txList = new ArrayList<>();
        txList.add(new TransactionResponse(1L, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD"));
        txList.add(new TransactionResponse(2L, new BigDecimal("50.00"), LocalDateTime.now().minusHours(1), "debit", "USD"));

        AccountDetailsResponse details = new AccountDetailsResponse(accountId, new BigDecimal("1000.00"), txList);

        when(accountService.getAccountDetails(accountId, 10)).thenReturn(details);

        mockMvc.perform(get("/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(accountId)))
                .andExpect(jsonPath("$.balance", is(1000.0)))
                .andExpect(jsonPath("$.recentTransactions", hasSize(2)))
                .andExpect(jsonPath("$.recentTransactions[0].id", is(1)))
                .andExpect(jsonPath("$.recentTransactions[0].amount", is(100.0)));

        verify(accountService).getAccountDetails(accountId, 10);
    }

    @DisplayName("GET Account Details - With custom recent transaction count")
    @Test
    void testGetAccount_CustomRecentTransactionCount() throws Exception {
        String accountId = "123456";
        int recentTx = 5;

        List<TransactionResponse> txList = new ArrayList<>();
        for (int i = 0; i < recentTx; i++) {
            txList.add(new TransactionResponse((long) i + 1, new BigDecimal("50.00"), LocalDateTime.now().minusHours(i), "credit", "USD"));
        }

        AccountDetailsResponse details = new AccountDetailsResponse(accountId, new BigDecimal("500.00"), txList);

        when(accountService.getAccountDetails(accountId, recentTx)).thenReturn(details);

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .param("recentTx", String.valueOf(recentTx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions", hasSize(recentTx)))
                .andExpect(jsonPath("$.balance", is(500.0)));

        verify(accountService).getAccountDetails(accountId, recentTx);
    }

    @DisplayName("GET Account Details - Non-existent account returns 404")
    @Test
    void testGetAccount_NonExistentAccount() throws Exception {
        String accountId = "999999";

        when(accountService.getAccountDetails(accountId, 10)).thenReturn(null);

        mockMvc.perform(get("/accounts/{accountId}", accountId))
                .andExpect(status().isNotFound());

        verify(accountService).getAccountDetails(accountId, 10);
    }

    @DisplayName("GET Account Details - Zero recent transactions")
    @Test
    void testGetAccount_ZeroRecentTransactions() throws Exception {
        String accountId = "123456";

        AccountDetailsResponse details = new AccountDetailsResponse(accountId, new BigDecimal("1000.00"), new ArrayList<>());

        when(accountService.getAccountDetails(accountId, 0)).thenReturn(details);

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .param("recentTx", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions", hasSize(0)))
                .andExpect(jsonPath("$.balance", is(1000.0)));

        verify(accountService).getAccountDetails(accountId, 0);
    }

    @DisplayName("GET Account Details - Large recent transaction count")
    @Test
    void testGetAccount_LargeRecentTransactionCount() throws Exception {
        String accountId = "123456";
        int recentTx = 100;

        List<TransactionResponse> txList = new ArrayList<>();
        for (int i = 0; i < recentTx; i++) {
            txList.add(new TransactionResponse((long) i + 1, new BigDecimal("10.00"), LocalDateTime.now().minusHours(i), "credit", "USD"));
        }

        AccountDetailsResponse details = new AccountDetailsResponse(accountId, new BigDecimal("5000.00"), txList);

        when(accountService.getAccountDetails(accountId, recentTx)).thenReturn(details);

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .param("recentTx", String.valueOf(recentTx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions", hasSize(recentTx)));

        verify(accountService).getAccountDetails(accountId, recentTx);
    }

    @DisplayName("POST Transaction - Different currency types")
    @Test
    void testPostTransaction_DifferentCurrencies() throws Exception {
        String accountId = "123456";

        // Test USD
        TransactionRequest usdReq = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
        TransactionResponse usdResp = new TransactionResponse(1L, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD");
        when(accountService.applyTransaction(accountId, usdReq)).thenReturn(usdResp);

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usdReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency", is("USD")));

        // Test INR
        reset(accountService);
        TransactionRequest inrReq = new TransactionRequest(new BigDecimal("5000.00"), "credit", "INR");
        TransactionResponse inrResp = new TransactionResponse(2L, new BigDecimal("5000.00"), LocalDateTime.now(), "credit", "INR");
        when(accountService.applyTransaction(accountId, inrReq)).thenReturn(inrResp);

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inrReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency", is("INR")));
    }

    @DisplayName("POST Transaction - Credit vs Debit transaction types")
    @Test
    void testPostTransaction_DifferentTransactionTypes() throws Exception {
        String accountId = "123456";

        // Test Credit
        TransactionRequest creditReq = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
        TransactionResponse creditResp = new TransactionResponse(1L, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD");
        when(accountService.applyTransaction(accountId, creditReq)).thenReturn(creditResp);

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("credit")));

        // Test Debit
        reset(accountService);
        TransactionRequest debitReq = new TransactionRequest(new BigDecimal("50.00"), "debit", "USD");
        TransactionResponse debitResp = new TransactionResponse(2L, new BigDecimal("50.00"), LocalDateTime.now(), "debit", "USD");
        when(accountService.applyTransaction(accountId, debitReq)).thenReturn(debitResp);

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("debit")));
    }

    @DisplayName("POST Transaction - Large decimal amount")
    @Test
    void testPostTransaction_LargeDecimalAmount() throws Exception {
        String accountId = "123456";
        BigDecimal largeAmount = new BigDecimal("999999999.99");
        
        TransactionRequest req = new TransactionRequest(largeAmount, "credit", "USD");
        TransactionResponse resp = new TransactionResponse(1L, largeAmount, LocalDateTime.now(), "credit", "USD");

        when(accountService.applyTransaction(accountId, req)).thenReturn(resp);

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(999999999.99)));

        verify(accountService).applyTransaction(accountId, req);
    }

    @DisplayName("GET Account Details - Empty transaction list")
    @Test
    void testGetAccount_EmptyTransactionList() throws Exception {
        String accountId = "123456";

        AccountDetailsResponse details = new AccountDetailsResponse(accountId, new BigDecimal("0.00"), new ArrayList<>());

        when(accountService.getAccountDetails(accountId, 10)).thenReturn(details);

        mockMvc.perform(get("/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(0.0)))
                .andExpect(jsonPath("$.recentTransactions", hasSize(0)));

        verify(accountService).getAccountDetails(accountId, 10);
    }

    @DisplayName("GET Balance - Very large balance")
    @Test
    void testGetBalance_VeryLargeBalance() throws Exception {
        String accountId = "123456";
        BigDecimal largeBalance = new BigDecimal("9999999999.99");

        when(accountService.getBalance(accountId)).thenReturn(largeBalance);

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("9999999999.99"));

        verify(accountService).getBalance(accountId);
    }
}
