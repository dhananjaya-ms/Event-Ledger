package com.dj.account.service;

import com.dj.account.dto.AccountDetailsResponse;
import com.dj.account.dto.TransactionRequest;
import com.dj.account.dto.TransactionResponse;
import com.dj.account.entity.Account;
import com.dj.account.entity.Transaction;
import com.dj.account.repository.AccountRepository;
import com.dj.account.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("AccountServiceImpl Unit Tests")
class AccountServiceImplTest {

    private AccountServiceImpl accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountService = new AccountServiceImpl(accountRepository, transactionRepository);
    }

    @DisplayName("Apply Transaction - Create transaction for new account")
    @Test
    void testApplyTransaction_NewAccount() {
        String accountId = "acc-001";
        BigDecimal amount = new BigDecimal("100.00");
        String type = "credit";
        String currency = "USD";

        TransactionRequest req = new TransactionRequest(amount, type, currency);

        Account newAccount = new Account(accountId, BigDecimal.ZERO);
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(newAccount);

        Transaction tx = new Transaction(newAccount, amount, LocalDateTime.now(), type, currency);
        tx.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

        TransactionResponse response = accountService.applyTransaction(accountId, req);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(amount, response.getAmount());
        assertEquals(type, response.getType());
        assertEquals(currency, response.getCurrency());
        
        verify(accountRepository).findById(accountId);
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @DisplayName("Apply Transaction - Add transaction to existing account")
    @Test
    void testApplyTransaction_ExistingAccount() {
        String accountId = "acc-001";
        BigDecimal initialBalance = new BigDecimal("500.00");
        BigDecimal amount = new BigDecimal("100.00");
        String type = "credit";
        String currency = "USD";

        TransactionRequest req = new TransactionRequest(amount, type, currency);

        Account existingAccount = new Account(accountId, initialBalance);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(existingAccount));

        Transaction tx = new Transaction(existingAccount, amount, LocalDateTime.now(), type, currency);
        tx.setId(2L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

        TransactionResponse response = accountService.applyTransaction(accountId, req);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals(amount, response.getAmount());
        
        verify(accountRepository).findById(accountId);
        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @DisplayName("Apply Transaction - Credit transaction increases balance")
    @Test
    void testApplyTransaction_CreditIncreaseBalance() {
        String accountId = "acc-002";
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal creditAmount = new BigDecimal("250.00");

        TransactionRequest req = new TransactionRequest(creditAmount, "credit", "USD");

        Account account = new Account(accountId, initialBalance);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Transaction tx = new Transaction(account, creditAmount, LocalDateTime.now(), "credit", "USD");
        tx.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

        accountService.applyTransaction(accountId, req);

        // Verify balance was updated
        verify(accountRepository).save(argThat(acc -> 
            acc.getBalance().equals(initialBalance.add(creditAmount))
        ));
    }

    @DisplayName("Apply Transaction - Debit transaction (negative amount)")
    @Test
    void testApplyTransaction_DebitDecreaseBalance() {
        String accountId = "acc-003";
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal debitAmount = new BigDecimal("-150.00");

        TransactionRequest req = new TransactionRequest(debitAmount, "debit", "USD");

        Account account = new Account(accountId, initialBalance);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Transaction tx = new Transaction(account, debitAmount, LocalDateTime.now(), "debit", "USD");
        tx.setId(2L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

        accountService.applyTransaction(accountId, req);

        verify(accountRepository).save(argThat(acc -> 
            acc.getBalance().equals(initialBalance.add(debitAmount))
        ));
    }

    @DisplayName("Apply Transaction - Multiple currencies")
    @Test
    void testApplyTransaction_MultipleCurrencies() {
        String accountId = "acc-004";

        // Test USD
        TransactionRequest usdReq = new TransactionRequest(new BigDecimal("100.00"), "credit", "USD");
        Account account1 = new Account(accountId, BigDecimal.ZERO);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account1));

        Transaction usdTx = new Transaction(account1, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD");
        usdTx.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(usdTx);

        TransactionResponse usdResp = accountService.applyTransaction(accountId, usdReq);
        assertEquals("USD", usdResp.getCurrency());

        // Test INR
        reset(accountRepository, transactionRepository);
        TransactionRequest inrReq = new TransactionRequest(new BigDecimal("5000.00"), "credit", "INR");
        Account account2 = new Account(accountId, new BigDecimal("100.00"));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account2));

        Transaction inrTx = new Transaction(account2, new BigDecimal("5000.00"), LocalDateTime.now(), "credit", "INR");
        inrTx.setId(2L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(inrTx);

        TransactionResponse inrResp = accountService.applyTransaction(accountId, inrReq);
        assertEquals("INR", inrResp.getCurrency());
    }

    @DisplayName("Get Balance - Existing account")
    @Test
    void testGetBalance_ExistingAccount() {
        String accountId = "acc-005";
        BigDecimal expectedBalance = new BigDecimal("2500.00");

        Account account = new Account(accountId, expectedBalance);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        BigDecimal balance = accountService.getBalance(accountId);

        assertEquals(expectedBalance, balance);
        verify(accountRepository).findById(accountId);
    }

    @DisplayName("Get Balance - Non-existent account returns zero")
    @Test
    void testGetBalance_NonExistentAccount() {
        String accountId = "acc-nonexistent";
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        BigDecimal balance = accountService.getBalance(accountId);

        assertEquals(BigDecimal.ZERO, balance);
        verify(accountRepository).findById(accountId);
    }

    @DisplayName("Get Account Details - With transactions")
    @Test
    void testGetAccountDetails_WithTransactions() {
        String accountId = "acc-006";
        BigDecimal balance = new BigDecimal("5000.00");

        Account account = new Account(accountId, balance);

        List<Transaction> txList = new ArrayList<>();
        Transaction tx1 = new Transaction(account, new BigDecimal("100.00"), LocalDateTime.now().minusHours(1), "credit", "USD");
        tx1.setId(1L);
        Transaction tx2 = new Transaction(account, new BigDecimal("50.00"), LocalDateTime.now().minusHours(2), "debit", "USD");
        tx2.setId(2L);
        txList.add(tx1);
        txList.add(tx2);

        Page<Transaction> page = new PageImpl<>(txList);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountOrderByTimestampDesc(account, PageRequest.of(0, 10)))
            .thenReturn(page);

        AccountDetailsResponse details = accountService.getAccountDetails(accountId, 10);

        assertNotNull(details);
        assertEquals(accountId, details.getAccountId());
        assertEquals(balance, details.getBalance());
        assertEquals(2, details.getRecentTransactions().size());
        verify(accountRepository).findById(accountId);
    }

    @DisplayName("Get Account Details - Custom recent transaction count")
    @Test
    void testGetAccountDetails_CustomTransactionCount() {
        String accountId = "acc-007";
        BigDecimal balance = new BigDecimal("1000.00");
        int recentTxCount = 5;

        Account account = new Account(accountId, balance);
        
        List<Transaction> txList = new ArrayList<>();
        for (int i = 0; i < recentTxCount; i++) {
            Transaction tx = new Transaction(account, new BigDecimal("10.00"), LocalDateTime.now().minusHours(i), "credit", "USD");
            tx.setId((long) i + 1);
            txList.add(tx);
        }

        Page<Transaction> page = new PageImpl<>(txList);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountOrderByTimestampDesc(account, PageRequest.of(0, recentTxCount)))
            .thenReturn(page);

        AccountDetailsResponse details = accountService.getAccountDetails(accountId, recentTxCount);

        assertNotNull(details);
        assertEquals(recentTxCount, details.getRecentTransactions().size());
        verify(transactionRepository).findByAccountOrderByTimestampDesc(account, PageRequest.of(0, recentTxCount));
    }

    @DisplayName("Get Account Details - Non-existent account returns null")
    @Test
    void testGetAccountDetails_NonExistentAccount() {
        String accountId = "acc-nonexistent";
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        AccountDetailsResponse details = accountService.getAccountDetails(accountId, 10);

        assertNull(details);
        verify(accountRepository).findById(accountId);
    }

    @DisplayName("Get Account Details - Zero transactions")
    @Test
    void testGetAccountDetails_ZeroRecentTransactions() {
        String accountId = "acc-008";
        BigDecimal balance = new BigDecimal("3000.00");

        Account account = new Account(accountId, balance);
        Page<Transaction> emptyPage = new PageImpl<>(new ArrayList<>());

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountOrderByTimestampDesc(account, PageRequest.of(0, 1)))
            .thenReturn(emptyPage);

        AccountDetailsResponse details = accountService.getAccountDetails(accountId, 0);

        assertNotNull(details);
        assertEquals(accountId, details.getAccountId());
        assertEquals(balance, details.getBalance());
        assertEquals(0, details.getRecentTransactions().size());
    }

    @DisplayName("Apply Transaction - Large decimal amounts")
    @Test
    void testApplyTransaction_LargeDecimalAmounts() {
        String accountId = "acc-009";
        BigDecimal largeAmount = new BigDecimal("999999999.99");

        TransactionRequest req = new TransactionRequest(largeAmount, "credit", "USD");

        Account account = new Account(accountId, BigDecimal.ZERO);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Transaction tx = new Transaction(account, largeAmount, LocalDateTime.now(), "credit", "USD");
        tx.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

        TransactionResponse response = accountService.applyTransaction(accountId, req);

        assertEquals(largeAmount, response.getAmount());
    }

    @DisplayName("Apply Transaction - Null balance handling")
    @Test
    void testApplyTransaction_NullBalanceHandling() {
        String accountId = "acc-010";
        BigDecimal amount = new BigDecimal("100.00");

        TransactionRequest req = new TransactionRequest(amount, "credit", "USD");

        Account account = new Account(accountId, null);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Transaction tx = new Transaction(account, amount, LocalDateTime.now(), "credit", "USD");
        tx.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(tx);

        TransactionResponse response = accountService.applyTransaction(accountId, req);

        assertNotNull(response);
        assertEquals(amount, response.getAmount());
        verify(accountRepository).save(argThat(acc -> 
            acc.getBalance() != null && acc.getBalance().equals(amount)
        ));
    }
}
