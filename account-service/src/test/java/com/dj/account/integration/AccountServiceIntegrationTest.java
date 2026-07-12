package com.dj.account.integration;

import com.dj.account.dto.AccountDetailsResponse;
import com.dj.account.dto.TransactionRequest;
import com.dj.account.entity.Account;
import com.dj.account.repository.AccountRepository;
import com.dj.account.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Account Service Integration Tests")
@TestPropertySource(locations = "classpath:application-test.properties")
class AccountServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @DisplayName("Integration - Complete flow: Create account, add transaction, retrieve balance")
    @Test
    @Transactional
    void testIntegration_CompleteFlow() throws Exception {
        String accountId = "111111";

        // Step 1: Add transaction (should create account if not exists)
        TransactionRequest txReq = new TransactionRequest(new BigDecimal("1000.00"), "credit", "USD");

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(1000.0)))
                .andExpect(jsonPath("$.type", is("credit")));

        // Step 2: Verify account was created with transaction
        Account savedAccount = accountRepository.findById(accountId).orElse(null);
        assert savedAccount != null;
        assert savedAccount.getBalance().equals(new BigDecimal("1000.00"));
        assert transactionRepository.findTop10ByAccountOrderByTimestampDesc(savedAccount).size() == 1;

        // Step 3: Get balance
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));

        // Step 4: Add another transaction (debit - negative amount)
        TransactionRequest txReq2 = new TransactionRequest(new BigDecimal("-500.00"), "debit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txReq2)))
                .andExpect(status().isCreated());

        // Step 5: Verify new balance
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("500.00"));
    }

    @DisplayName("Integration - Multiple accounts with independent balances")
    @Test
    @Transactional
    void testIntegration_MultipleAccounts() throws Exception {
        String account1 = "222222";
        String account2 = "333333";

        // Create transactions for account 1
        TransactionRequest tx1 = new TransactionRequest(new BigDecimal("1000.00"), "credit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", account1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx1)))
                .andExpect(status().isCreated());

        // Create transactions for account 2
        TransactionRequest tx2 = new TransactionRequest(new BigDecimal("5000.00"), "credit", "INR");
        mockMvc.perform(post("/accounts/{accountId}/transactions", account2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx2)))
                .andExpect(status().isCreated());

        // Verify account 1 balance
        mockMvc.perform(get("/accounts/{accountId}/balance", account1))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));

        // Verify account 2 balance
        mockMvc.perform(get("/accounts/{accountId}/balance", account2))
                .andExpect(status().isOk())
                .andExpect(content().string("5000.00"));
    }

    @DisplayName("Integration - Sequential credit and debit transactions")
    @Test
    @Transactional
    void testIntegration_SequentialTransactions() throws Exception {
        String accountId = "444444";

        // Credit 1000
        TransactionRequest creditReq = new TransactionRequest(new BigDecimal("1000.00"), "credit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));

        // Debit 300
        TransactionRequest debitReq1 = new TransactionRequest(new BigDecimal("-300.00"), "debit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitReq1)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("700.00"));

        // Debit 200
        TransactionRequest debitReq2 = new TransactionRequest(new BigDecimal("-200.00"), "debit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitReq2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("500.00"));
    }

    @DisplayName("Integration - Get account details with transaction history")
    @Test
    @Transactional
    void testIntegration_GetAccountDetailsWithHistory() throws Exception {
        String accountId = "555555";

        // Add 3 transactions
        for (int i = 0; i < 3; i++) {
            TransactionRequest txReq = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
            mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(txReq)))
                    .andExpect(status().isCreated());
        }

        // Get account details
        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .param("recentTx", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(accountId)))
                .andExpect(jsonPath("$.balance", is(300.0)))
                .andExpect(jsonPath("$.recentTransactions", hasSize(3)))
                .andExpect(jsonPath("$.recentTransactions[0].amount", is(100.0)))
                .andExpect(jsonPath("$.recentTransactions[0].type", is("credit")));
    }

    @DisplayName("Integration - Get account details with limited transaction count")
    @Test
    @Transactional
    void testIntegration_GetAccountDetailsLimitedTransactions() throws Exception {
        String accountId = "666666";

        // Add 5 transactions
        for (int i = 0; i < 5; i++) {
            TransactionRequest txReq = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
            mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(txReq)))
                    .andExpect(status().isCreated());
        }

        // Get only 3 recent transactions
        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .param("recentTx", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions", hasSize(3)))
                .andExpect(jsonPath("$.balance", is(500.0)));
    }

    @DisplayName("Integration - Mixed currency transactions")
    @Test
    @Transactional
    void testIntegration_MixedCurrencies() throws Exception {
        String accountId = "777777";

        // Add USD transaction
        TransactionRequest usdReq = new TransactionRequest(new BigDecimal("1000.00"), "credit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usdReq)))
                .andExpect(status().isCreated());

        // Add INR transaction
        TransactionRequest inrReq = new TransactionRequest(new BigDecimal("5000.00"), "credit", "INR");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inrReq)))
                .andExpect(status().isCreated());

        // Get account details and verify both transactions are present
        mockMvc.perform(get("/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions", hasSize(2)))
                .andExpect(jsonPath("$.recentTransactions[*].currency", containsInAnyOrder("USD", "INR")))
                .andExpect(jsonPath("$.balance", is(6000.0)));
    }

    @DisplayName("Integration - New account starts with zero balance")
    @Test
    @Transactional
    void testIntegration_NewAccountZeroBalance() throws Exception {
        String accountId = "888888";

        // Get balance before any transactions
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @DisplayName("Integration - Account goes negative with debits")
    @Test
    @Transactional
    void testIntegration_NegativeBalance() throws Exception {
        String accountId = "999999";

        // Add small credit
        TransactionRequest creditReq = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isCreated());

        // Add large debit
        TransactionRequest debitReq = new TransactionRequest(new BigDecimal("-500.00"), "debit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isCreated());

        // Verify negative balance
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("-400.00"));
    }

    @DisplayName("Integration - Large number of transactions")
    @Test
    @Transactional
    void testIntegration_ManyTransactions() throws Exception {
        String accountId = "101010";

        // Add 20 transactions
        for (int i = 0; i < 20; i++) {
            TransactionRequest txReq = new TransactionRequest(new BigDecimal("50.00"), "credit", "USD");
            mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(txReq)))
                    .andExpect(status().isCreated());
        }

        // Verify balance
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));

        // Get only 10 most recent transactions
        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .param("recentTx", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions", hasSize(10)))
                .andExpect(jsonPath("$.balance", is(1000.0)));
    }

    @DisplayName("Integration - Transaction timestamp ordering")
    @Test
    @Transactional
    void testIntegration_TransactionTimestampOrdering() throws Exception {
        String accountId = "111213";

        // Add first transaction
        TransactionRequest tx1 = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx1)))
                .andExpect(status().isCreated());

        Thread.sleep(100); // Small delay to ensure different timestamps

        // Add second transaction
        TransactionRequest tx2 = new TransactionRequest(new BigDecimal("200.00"), "credit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx2)))
                .andExpect(status().isCreated());

        // Get account details - should be ordered by timestamp DESC (most recent first)
        mockMvc.perform(get("/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions[0].amount", is(200.0)))
                .andExpect(jsonPath("$.recentTransactions[1].amount", is(100.0)));
    }

    @DisplayName("Integration - Precision with large decimal amounts")
    @Test
    @Transactional
    void testIntegration_LargeDecimalPrecision() throws Exception {
        String accountId = "121314";

        // Add large decimal transaction
        TransactionRequest largeReq = new TransactionRequest(new BigDecimal("123456.78"), "credit", "USD");
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeReq)))
                .andExpect(status().isCreated());

        // Verify balance preserves precision
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string("123456.78"));
    }
}
