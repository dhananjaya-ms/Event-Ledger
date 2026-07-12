package com.dj.account.repository;

import com.dj.account.entity.Account;
import com.dj.account.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Repository Layer Tests")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @DisplayName("AccountRepository - Save and retrieve account")
    @Test
    void testSaveAndRetrieveAccount() {
        Account account = new Account("acc-001", new BigDecimal("1000.00"));

        Account saved = accountRepository.save(account);

        assertNotNull(saved.getId());
        assertEquals("acc-001", saved.getId());
        assertEquals(new BigDecimal("1000.00"), saved.getBalance());

        Optional<Account> retrieved = accountRepository.findById("acc-001");
        assertTrue(retrieved.isPresent());
        assertEquals("acc-001", retrieved.get().getId());
        assertEquals(new BigDecimal("1000.00"), retrieved.get().getBalance());
    }

    @DisplayName("AccountRepository - Update account balance")
    @Test
    void testUpdateAccountBalance() {
        Account account = new Account("acc-002", new BigDecimal("500.00"));
        accountRepository.save(account);

        // Update balance
        Account toUpdate = accountRepository.findById("acc-002").get();
        toUpdate.setBalance(new BigDecimal("750.00"));
        accountRepository.save(toUpdate);

        Account updated = accountRepository.findById("acc-002").get();
        assertEquals(new BigDecimal("750.00"), updated.getBalance());
    }

    @DisplayName("AccountRepository - Non-existent account returns empty")
    @Test
    void testNonExistentAccount() {
        Optional<Account> result = accountRepository.findById("non-existent");
        assertFalse(result.isPresent());
    }

    @DisplayName("AccountRepository - Delete account")
    @Test
    void testDeleteAccount() {
        Account account = new Account("acc-003", new BigDecimal("1000.00"));
        accountRepository.save(account);

        accountRepository.deleteById("acc-003");

        Optional<Account> result = accountRepository.findById("acc-003");
        assertFalse(result.isPresent());
    }

    @DisplayName("TransactionRepository - Save and retrieve transaction")
    @Test
    void testSaveAndRetrieveTransaction() {
        Account account = new Account("acc-004", BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(account);

        Transaction tx = new Transaction(savedAccount, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD");
        Transaction savedTx = transactionRepository.save(tx);

        assertNotNull(savedTx.getId());
        assertEquals(savedAccount.getId(), savedTx.getAccount().getId());
        assertEquals(new BigDecimal("100.00"), savedTx.getAmount());
        assertEquals("credit", savedTx.getType());
        assertEquals("USD", savedTx.getCurrency());
    }

    @DisplayName("TransactionRepository - Find transactions by account ordered by timestamp DESC")
    @Test
    void testFindByAccountOrderByTimestampDesc() throws InterruptedException {
        Account account = new Account("acc-005", BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(account);

        // Create transactions with slight delays to ensure different timestamps
        LocalDateTime now = LocalDateTime.now();
        
        Transaction tx1 = new Transaction(savedAccount, new BigDecimal("100.00"), now.minusHours(2), "credit", "USD");
        Transaction tx2 = new Transaction(savedAccount, new BigDecimal("200.00"), now.minusHours(1), "debit", "USD");
        Transaction tx3 = new Transaction(savedAccount, new BigDecimal("50.00"), now, "credit", "USD");

        transactionRepository.save(tx1);
        transactionRepository.save(tx2);
        transactionRepository.save(tx3);

        // Query with pagination
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> result = transactionRepository.findByAccountOrderByTimestampDesc(savedAccount, pageable);

        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        
        // Should be ordered DESC (most recent first)
        assertEquals(new BigDecimal("50.00"), result.getContent().get(0).getAmount());
        assertEquals(new BigDecimal("200.00"), result.getContent().get(1).getAmount());
        assertEquals(new BigDecimal("100.00"), result.getContent().get(2).getAmount());
    }

    @DisplayName("TransactionRepository - Find transactions with pagination")
    @Test
    void testFindByAccountWithPagination() {
        Account account = new Account("acc-006", BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(account);

        // Create 15 transactions
        for (int i = 0; i < 15; i++) {
            Transaction tx = new Transaction(
                savedAccount, 
                new BigDecimal(String.valueOf(i * 10)), 
                LocalDateTime.now().minusHours(i), 
                "credit", 
                "USD"
            );
            transactionRepository.save(tx);
        }

        // Get first page (5 items)
        Pageable pageable = PageRequest.of(0, 5);
        Page<Transaction> page1 = transactionRepository.findByAccountOrderByTimestampDesc(savedAccount, pageable);

        assertEquals(5, page1.getContent().size());
        assertEquals(15, page1.getTotalElements());
        assertEquals(3, page1.getTotalPages());
        assertTrue(page1.isFirst());
        assertFalse(page1.isLast());

        // Get second page (5 items)
        pageable = PageRequest.of(1, 5);
        Page<Transaction> page2 = transactionRepository.findByAccountOrderByTimestampDesc(savedAccount, pageable);

        assertEquals(5, page2.getContent().size());
        assertFalse(page2.isFirst());
        assertFalse(page2.isLast());

        // Get third page (5 items)
        pageable = PageRequest.of(2, 5);
        Page<Transaction> page3 = transactionRepository.findByAccountOrderByTimestampDesc(savedAccount, pageable);

        assertEquals(5, page3.getContent().size());
        assertTrue(page3.isLast());
    }

    @DisplayName("TransactionRepository - Find top 10 transactions by account")
    @Test
    void testFindTop10ByAccountOrderByTimestampDesc() {
        Account account = new Account("acc-007", BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(account);

        // Create 15 transactions
        for (int i = 0; i < 15; i++) {
            Transaction tx = new Transaction(
                savedAccount, 
                new BigDecimal(String.valueOf(i * 10)), 
                LocalDateTime.now().minusHours(i), 
                "credit", 
                "USD"
            );
            transactionRepository.save(tx);
        }

        List<Transaction> result = transactionRepository.findTop10ByAccountOrderByTimestampDesc(savedAccount);

        assertEquals(10, result.size());
        // Most recent should be first
        assertEquals(new BigDecimal("0"), result.get(0).getAmount());
        assertEquals(new BigDecimal("90"), result.get(9).getAmount());
    }

    @DisplayName("TransactionRepository - Multiple transactions different accounts")
    @Test
    void testMultipleAccountsIndependentTransactions() {
        Account account1 = new Account("acc-008", BigDecimal.ZERO);
        Account account2 = new Account("acc-009", BigDecimal.ZERO);

        Account savedAcc1 = accountRepository.save(account1);
        Account savedAcc2 = accountRepository.save(account2);

        // Add transactions to account 1
        Transaction tx1a = new Transaction(savedAcc1, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD");
        Transaction tx1b = new Transaction(savedAcc1, new BigDecimal("50.00"), LocalDateTime.now().minusHours(1), "debit", "USD");
        transactionRepository.save(tx1a);
        transactionRepository.save(tx1b);

        // Add transactions to account 2
        Transaction tx2a = new Transaction(savedAcc2, new BigDecimal("200.00"), LocalDateTime.now(), "credit", "INR");
        transactionRepository.save(tx2a);

        // Query account 1 transactions
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> acc1Txs = transactionRepository.findByAccountOrderByTimestampDesc(savedAcc1, pageable);

        // Query account 2 transactions
        Page<Transaction> acc2Txs = transactionRepository.findByAccountOrderByTimestampDesc(savedAcc2, pageable);

        assertEquals(2, acc1Txs.getTotalElements());
        assertEquals(1, acc2Txs.getTotalElements());
        assertEquals("USD", acc1Txs.getContent().get(0).getCurrency());
        assertEquals("INR", acc2Txs.getContent().get(0).getCurrency());
    }

    @DisplayName("TransactionRepository - Empty transaction list for new account")
    @Test
    void testEmptyTransactionListForNewAccount() {
        Account account = new Account("acc-010", BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(account);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> result = transactionRepository.findByAccountOrderByTimestampDesc(savedAccount, pageable);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @DisplayName("TransactionRepository - Different transaction types and currencies")
    @Test
    void testDifferentTransactionTypesAndCurrencies() {
        Account account = new Account("acc-011", BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(account);

        // Create transactions with different types and currencies
        Transaction creditUSD = new Transaction(savedAccount, new BigDecimal("100.00"), LocalDateTime.now(), "credit", "USD");
        Transaction debitUSD = new Transaction(savedAccount, new BigDecimal("50.00"), LocalDateTime.now().minusHours(1), "debit", "USD");
        Transaction creditINR = new Transaction(savedAccount, new BigDecimal("5000.00"), LocalDateTime.now().minusHours(2), "credit", "INR");

        transactionRepository.save(creditUSD);
        transactionRepository.save(debitUSD);
        transactionRepository.save(creditINR);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> result = transactionRepository.findByAccountOrderByTimestampDesc(savedAccount, pageable);

        assertEquals(3, result.getTotalElements());
        
        // Verify mixed types and currencies are preserved
        List<Transaction> txs = result.getContent();
        assertTrue(txs.stream().anyMatch(t -> "credit".equals(t.getType())));
        assertTrue(txs.stream().anyMatch(t -> "debit".equals(t.getType())));
        assertTrue(txs.stream().anyMatch(t -> "USD".equals(t.getCurrency())));
        assertTrue(txs.stream().anyMatch(t -> "INR".equals(t.getCurrency())));
    }

    @DisplayName("AccountRepository - Account with null balance defaults to zero")
    @Test
    void testAccountNullBalanceDefaultsToZero() {
        Account account = new Account("acc-012", null);
        Account saved = accountRepository.save(account);

        Account retrieved = accountRepository.findById("acc-012").get();
        assertNotNull(retrieved.getBalance());
        assertEquals(BigDecimal.ZERO, retrieved.getBalance());
    }

    @DisplayName("TransactionRepository - Large decimal amounts precision")
    @Test
    void testLargeDecimalAmountsPrecision() {
        Account account = new Account("acc-013", BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(account);

        BigDecimal largeAmount = new BigDecimal("999999999.99");
        Transaction tx = new Transaction(savedAccount, largeAmount, LocalDateTime.now(), "credit", "USD");
        Transaction savedTx = transactionRepository.save(tx);

        Transaction retrieved = transactionRepository.findById(savedTx.getId()).get();
        assertEquals(largeAmount, retrieved.getAmount());
    }
}
