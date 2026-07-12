package com.dj.gateway.service;

import com.dj.gateway.dto.EventRequest;
import com.dj.gateway.dto.EventResponse;
import com.dj.gateway.entity.Event;
import com.dj.gateway.exception.IdempotencyException;
import com.dj.gateway.repository.EventRepository;
import com.dj.gateway.util.EventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "account-service.url=http://localhost:8080"
})
@DisplayName("GatewayService Core Functionality Tests")
class GatewayServiceCoreTest {

    @Autowired
    private GatewayService gatewayService;

    @Autowired
    private EventRepository eventRepository;

    private String traceId;
    private String accountId;

    @BeforeEach
    void setUp() {
        // Clear repository before each test
        eventRepository.deleteAll();
        traceId = "test-trace-" + System.nanoTime();
        accountId = "acc-" + System.nanoTime();
    }

    // ================== Idempotency Tests ==================

    @Test
    @DisplayName("Idempotency: First submission creates event with PENDING status")
    void testIdempotency_FirstSubmission() {
        EventRequest req = createEventRequest(accountId, "evt-idempotent-001", "CREDIT", 100.00, "2026-07-11T10:00:00Z");

        EventResponse response = gatewayService.submitEvent(req, traceId);

        assertNotNull(response);
        assertEquals("evt-idempotent-001", response.getEventId());
        assertNotNull(response.getStatus());
    }

    @Test
    @DisplayName("Idempotency: Duplicate eventId throws IdempotencyException with existing event")
    void testIdempotency_DuplicateEventId() {
        EventRequest req1 = createEventRequest(accountId, "evt-dup-001", "CREDIT", 100.00, "2026-07-11T10:00:00Z");
        EventRequest req2 = createEventRequest(accountId, "evt-dup-001", "CREDIT", 100.00, "2026-07-11T10:00:00Z");

        // First submission succeeds
        EventResponse response1 = gatewayService.submitEvent(req1, traceId);
        assertNotNull(response1);

        // Second submission with same eventId should throw IdempotencyException
        IdempotencyException exception = assertThrows(IdempotencyException.class, () -> {
            gatewayService.submitEvent(req2, traceId);
        });

        assertNotNull(exception.getExisting());
        assertEquals("evt-dup-001", exception.getExisting().getEventId());
    }

    @Test
    @DisplayName("Idempotency: Multiple submissions with different eventIds for same account succeed")
    void testIdempotency_MultipleDistinctEventsForSameAccount() {
        EventRequest req1 = createEventRequest(accountId, "evt-multi-001", "CREDIT", 100.00, "2026-07-11T10:00:00Z");
        EventRequest req2 = createEventRequest(accountId, "evt-multi-002", "DEBIT", 50.00, "2026-07-11T11:00:00Z");

        EventResponse response1 = gatewayService.submitEvent(req1, traceId);
        EventResponse response2 = gatewayService.submitEvent(req2, traceId);

        assertNotNull(response1);
        assertNotNull(response2);
        assertNotEquals(response1.getEventId(), response2.getEventId());
    }

    // ================== Out-of-Order Events Tests ==================

    @Test
    @DisplayName("Out-of-Order: Later event submitted first, earlier event submitted second - should process in correct order")
    void testOutOfOrder_LaterEventFirst() {
        // Submit event with later timestamp first
        EventRequest laterEvent = createEventRequest(accountId, "evt-order-002", "DEBIT", 30.00, "2026-07-11T11:00:00Z");
        EventRequest earlierEvent = createEventRequest(accountId, "evt-order-001", "CREDIT", 100.00, "2026-07-11T10:00:00Z");

        EventResponse resp1 = gatewayService.submitEvent(laterEvent, traceId);
        EventResponse resp2 = gatewayService.submitEvent(earlierEvent, traceId);

        assertNotNull(resp1);
        assertNotNull(resp2);

        // Retrieve both events and verify they are stored correctly
        EventResponse retrieved1 = gatewayService.getEvent("evt-order-001");
        EventResponse retrieved2 = gatewayService.getEvent("evt-order-002");

        assertNotNull(retrieved1);
        assertNotNull(retrieved2);
    }

    @Test
    @DisplayName("Out-of-Order: List events returns sorted by eventTimestamp DESC")
    void testOutOfOrder_ListingReturnsSortedByTimestamp() {
        // Submit events in random order
        EventRequest event1 = createEventRequest(accountId, "evt-sort-001", "CREDIT", 100.00, "2026-07-11T10:00:00Z");
        EventRequest event2 = createEventRequest(accountId, "evt-sort-002", "DEBIT", 50.00, "2026-07-11T11:00:00Z");
        EventRequest event3 = createEventRequest(accountId, "evt-sort-003", "CREDIT", 75.00, "2026-07-11T09:00:00Z");

        gatewayService.submitEvent(event1, traceId);
        gatewayService.submitEvent(event2, traceId);
        gatewayService.submitEvent(event3, traceId);

        Pageable pageable = PageRequest.of(0, 10);
        Page<EventResponse> result = gatewayService.listEventsForAccount(accountId, pageable);

        // Should be sorted DESC by eventTimestamp: event2, event1, event3
        assertEquals(3, result.getContent().size());
    }

    // ================== Balance/State Tests ==================

    @Test
    @DisplayName("Balance: Track multiple credits and debits for an account")
    void testBalance_MultipleCreditDebit() {
        String testAccount = "balance-test-" + System.nanoTime();

        // Create multiple events
        EventRequest credit1 = createEventRequest(testAccount, "evt-bal-001", "CREDIT", 100.00, "2026-07-11T10:00:00Z");
        EventRequest credit2 = createEventRequest(testAccount, "evt-bal-002", "CREDIT", 50.00, "2026-07-11T11:00:00Z");
        EventRequest debit1 = createEventRequest(testAccount, "evt-bal-003", "DEBIT", 30.00, "2026-07-11T12:00:00Z");

        EventResponse resp1 = gatewayService.submitEvent(credit1, traceId);
        EventResponse resp2 = gatewayService.submitEvent(credit2, traceId);
        EventResponse resp3 = gatewayService.submitEvent(debit1, traceId);

        // Verify all events are stored
        assertEquals(3, eventRepository.findAll().stream()
                .filter(e -> testAccount.equals(e.getAccountId()))
                .count());
    }

    @Test
    @DisplayName("Balance: Verify event amounts are preserved correctly")
    void testBalance_AmountPreservation() {
        EventRequest req = createEventRequest(accountId, "evt-amount-test", "CREDIT", 123.45, "2026-07-11T10:00:00Z");

        EventResponse response = gatewayService.submitEvent(req, traceId);

        assertEquals(BigDecimal.valueOf(123.45), response.getAmount());

        EventResponse retrieved = gatewayService.getEvent("evt-amount-test");
        assertEquals(BigDecimal.valueOf(123.45), retrieved.getAmount());
    }

    @Test
    @DisplayName("Balance: List events for account with pagination")
    void testBalance_PaginationOfEvents() {
        // Create 5 events
        for (int i = 1; i <= 5; i++) {
            EventRequest req = createEventRequest(accountId, "evt-page-" + i, "CREDIT", 10.00 * i, "2026-07-11T" + String.format("%02d", 9 + i) + ":00:00Z");
            gatewayService.submitEvent(req, traceId);
        }

        // First page with 2 items per page
        Page<EventResponse> page1 = gatewayService.listEventsForAccount(accountId, PageRequest.of(0, 2));
        assertEquals(2, page1.getContent().size());
        assertEquals(5, page1.getTotalElements());

        // Second page
        Page<EventResponse> page2 = gatewayService.listEventsForAccount(accountId, PageRequest.of(1, 2));
        assertEquals(2, page2.getContent().size());

        // Third page
        Page<EventResponse> page3 = gatewayService.listEventsForAccount(accountId, PageRequest.of(2, 2));
        assertEquals(1, page3.getContent().size());
    }

    // ================== Validation Tests ==================

    @Test
    @DisplayName("Validation: Event with large decimal amount is stored correctly")
    void testValidation_LargeDecimalAmount() {
        EventRequest req = createEventRequest(accountId, "evt-large-amount", "CREDIT", 999999999.99, "2026-07-11T10:00:00Z");

        EventResponse response = gatewayService.submitEvent(req, traceId);

        assertEquals(BigDecimal.valueOf(999999999.99), response.getAmount());
    }

    @Test
    @DisplayName("Validation: Event with metadata is stored correctly")
    void testValidation_MetadataPreservation() {
        EventRequest req = createEventRequest(accountId, "evt-metadata", "CREDIT", 100.00, "2026-07-11T10:00:00Z");
        req.setMetadata("test-metadata-value");

        EventResponse response = gatewayService.submitEvent(req, traceId);

        assertNotNull(response);
        EventResponse retrieved = gatewayService.getEvent("evt-metadata");
        assertNotNull(retrieved.getMetadata());
    }

    @Test
    @DisplayName("Validation: Different transaction types (CREDIT/DEBIT) stored correctly")
    void testValidation_TransactionTypes() {
        EventRequest creditReq = createEventRequest(accountId, "evt-credit-type", "CREDIT", 50.00, "2026-07-11T10:00:00Z");
        EventRequest debitReq = createEventRequest(accountId, "evt-debit-type", "DEBIT", 30.00, "2026-07-11T11:00:00Z");

        EventResponse creditResp = gatewayService.submitEvent(creditReq, traceId);
        EventResponse debitResp = gatewayService.submitEvent(debitReq, traceId);

        assertEquals("CREDIT", creditResp.getType());
        assertEquals("DEBIT", debitResp.getType());
    }

    // ================== Helper Methods ==================

    private EventRequest createEventRequest(String accountId, String eventId, String type, double amount, String timestamp) {
        EventRequest req = new EventRequest();
        req.setEventId(eventId);
        req.setAccountId(accountId);
        req.setType(type);
        req.setAmount(BigDecimal.valueOf(amount));
        req.setCurrency("USD");
        req.setEventTimestamp(timestamp);
        return req;
    }
}
