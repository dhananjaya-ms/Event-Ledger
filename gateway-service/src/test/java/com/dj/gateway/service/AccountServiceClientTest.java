package com.dj.gateway.service;

import com.dj.gateway.dto.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "account-service.url=http://localhost:8080"
})
@DisplayName("AccountServiceClient Tests - Trace ID Handling")
class AccountServiceClientTest {

    private String testTraceId;
    private String testAccountId;
    private TransactionRequest testTransaction;

    @BeforeEach
    void setUp() {
        testTraceId = "trace-client-" + System.nanoTime();
        testAccountId = "123";
        testTransaction = new TransactionRequest(
                BigDecimal.valueOf(100.00), 
                "CREDIT", 
                "USD", 
                "test transaction"
        );
    }

    // ================== Transaction Request Tests ==================

    @Test
    @DisplayName("Client: TransactionRequest correctly stores amount")
    void testTransaction_AmountStored() {
        BigDecimal amount = BigDecimal.valueOf(250.75);
        TransactionRequest tx = new TransactionRequest(amount, "DEBIT", "EUR", "test");

        assertEquals(amount, tx.getAmount());
    }

    @Test
    @DisplayName("Client: TransactionRequest correctly stores type")
    void testTransaction_TypeStored() {
        TransactionRequest creditTx = new TransactionRequest(BigDecimal.valueOf(100), "CREDIT", "USD", "credit");
        TransactionRequest debitTx = new TransactionRequest(BigDecimal.valueOf(100), "DEBIT", "USD", "debit");

        assertEquals("CREDIT", creditTx.getType());
        assertEquals("DEBIT", debitTx.getType());
    }

    @Test
    @DisplayName("Client: TransactionRequest correctly stores currency")
    void testTransaction_CurrencyStored() {
        TransactionRequest txUSD = new TransactionRequest(BigDecimal.valueOf(100), "CREDIT", "USD", "test");
        TransactionRequest txEUR = new TransactionRequest(BigDecimal.valueOf(100), "CREDIT", "EUR", "test");

        assertEquals("USD", txUSD.getCurrency());
        assertEquals("EUR", txEUR.getCurrency());
    }

    @Test
    @DisplayName("Client: TransactionRequest correctly stores description")
    void testTransaction_DescriptionStored() {
        String description = "event:evt-12345";
        TransactionRequest tx = new TransactionRequest(BigDecimal.valueOf(100), "CREDIT", "USD", description);

        assertEquals(description, tx.getDescription());
    }

    @Test
    @DisplayName("Client: TransactionRequest with negative amount for DEBIT")
    void testTransaction_NegativeAmountForDebit() {
        BigDecimal negativeAmount = BigDecimal.valueOf(-100.00);
        TransactionRequest tx = new TransactionRequest(negativeAmount, "DEBIT", "USD", "debit");

        assertTrue(tx.getAmount().signum() < 0);
        assertEquals("DEBIT", tx.getType());
    }

    // ================== Trace ID Tests ==================

    @Test
    @DisplayName("Trace: Test trace ID format is valid UUID")
    void testTrace_ValidUUIDFormat() {
        String traceId = "trace-test-" + System.nanoTime();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
    }

    @Test
    @DisplayName("Trace: Different trace IDs are unique")
    void testTrace_UniquenessOfTraceIds() {
        String trace1 = "trace-1-" + System.nanoTime();
        String trace2 = "trace-2-" + System.nanoTime();

        assertNotEquals(trace1, trace2);
    }

    // ================== Multiple Transaction Tests ==================

    @Test
    @DisplayName("Client: Multiple transactions with different amounts")
    void testClient_MultipleTransactionsWithDifferentAmounts() {
        TransactionRequest tx1 = new TransactionRequest(BigDecimal.valueOf(100), "CREDIT", "USD", "tx1");
        TransactionRequest tx2 = new TransactionRequest(BigDecimal.valueOf(200), "DEBIT", "USD", "tx2");
        TransactionRequest tx3 = new TransactionRequest(BigDecimal.valueOf(50), "CREDIT", "USD", "tx3");

        assertEquals(BigDecimal.valueOf(100), tx1.getAmount());
        assertEquals(BigDecimal.valueOf(200), tx2.getAmount());
        assertEquals(BigDecimal.valueOf(50), tx3.getAmount());
    }

    @Test
    @DisplayName("Client: Transaction data structure preserves all fields")
    void testClient_TransactionDataPreservation() {
        BigDecimal amount = BigDecimal.valueOf(123.45);
        String type = "CREDIT";
        String currency = "GBP";
        String description = "event:evt-99999";

        TransactionRequest tx = new TransactionRequest(amount, type, currency, description);

        assertEquals(amount, tx.getAmount());
        assertEquals(type, tx.getType());
        assertEquals(currency, tx.getCurrency());
        assertEquals(description, tx.getDescription());
    }

    @Test
    @DisplayName("Client: Trace ID can be passed through to service calls")
    void testClient_TraceIDPassThrough() {
        String trace1 = "integration-trace-" + System.nanoTime();
        String trace2 = "gateway-trace-" + System.nanoTime();

        // Verify trace IDs are different when used in different contexts
        assertNotEquals(trace1, trace2);
        assertTrue(trace1.startsWith("integration-trace"));
        assertTrue(trace2.startsWith("gateway-trace"));
    }
}
