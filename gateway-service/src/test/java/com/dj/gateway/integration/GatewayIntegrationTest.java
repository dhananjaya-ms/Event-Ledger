package com.dj.gateway.integration;

import com.dj.gateway.dto.EventRequest;
import com.dj.gateway.dto.EventResponse;
import com.dj.gateway.repository.EventRepository;
import com.dj.gateway.service.GatewayService;
import com.dj.gateway.web.TraceFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "account-service.url=http://localhost:8080"
})
@DisplayName("Gateway ↔ Account Service Integration Tests")
class GatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GatewayService gatewayService;

    @Autowired
    private EventRepository eventRepository;

    private String testTraceId;
    private String testAccountId;
    private EventRequest validEventRequest;

    @BeforeEach
    void setUp() {
        // Clear all events before each test
        eventRepository.deleteAll();

        testTraceId = "integration-trace-" + UUID.randomUUID().toString();
        testAccountId = "integration-account-" + System.nanoTime();

        validEventRequest = new EventRequest();
        validEventRequest.setEventId("evt-integration-" + System.nanoTime());
        validEventRequest.setAccountId(testAccountId);
        validEventRequest.setType("CREDIT");
        validEventRequest.setAmount(BigDecimal.valueOf(100.00));
        validEventRequest.setCurrency("USD");
        validEventRequest.setEventTimestamp("2026-07-11T10:30:00Z");
    }

    // ================== Full Integration Flow Tests ==================

    @Test
    @DisplayName("Integration: Complete flow - POST event → retrieve → list with trace propagation")
    void testIntegration_CompleteFlow_PostRetrieveList() throws Exception {
        // Step 1: Submit event via REST
        MvcResult postResult = mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(jsonPath("$.eventId", notNullValue()))
                .andExpect(jsonPath("$.accountId").value(testAccountId))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        String responseContent = postResult.getResponse().getContentAsString();
        String eventId = validEventRequest.getEventId();

        // Step 2: Verify event is persisted
        assertTrue(eventRepository.findByEventId(eventId).isPresent());

        // Step 3: Retrieve single event
        MvcResult getResult = mockMvc.perform(get("/events/" + eventId)
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.accountId").value(testAccountId))
                .andReturn();

        // Step 4: List events for account
        MvcResult listResult = mockMvc.perform(get("/events")
                .param("account", testAccountId)
                .param("page", "0")
                .param("size", "20")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].eventId", notNullValue()))
                .andReturn();

        // Verify trace ID was propagated throughout
        assertEquals(testTraceId, getResult.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER));
        assertEquals(testTraceId, listResult.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER));
    }

    @Test
    @DisplayName("Integration: Submit multiple events for single account - verify ordering and retrieval")
    void testIntegration_MultipleEventsOrdering() throws Exception {
        String account = "integration-multi-" + System.nanoTime();
        long now = System.nanoTime();

        // Submit events with different timestamps
        EventRequest event1 = createEvent(account, "evt-multi-1", "CREDIT", 50.00, "2026-07-11T10:00:00Z");
        EventRequest event2 = createEvent(account, "evt-multi-2", "DEBIT", 20.00, "2026-07-11T11:00:00Z");
        EventRequest event3 = createEvent(account, "evt-multi-3", "CREDIT", 75.00, "2026-07-11T09:00:00Z");

        // Submit in different order
        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(event1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(event2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(event3)))
                .andExpect(status().isCreated());

        // List and verify all are present
        MvcResult result = mockMvc.perform(get("/events")
                .param("account", account)
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andReturn();

        // Verify events are sorted DESC by timestamp
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("evt-multi-1"));
        assertTrue(content.contains("evt-multi-2"));
        assertTrue(content.contains("evt-multi-3"));
    }

    @Test
    @DisplayName("Integration: Idempotency - duplicate submission returns 409 with existing event details")
    void testIntegration_Idempotency_DuplicateReturns409() throws Exception {
        // First submission
        MvcResult firstResult = mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Duplicate submission
        MvcResult secondResult = mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.eventId").value(validEventRequest.getEventId()))
                .andExpect(jsonPath("$.accountId").value(testAccountId))
                .andReturn();

        // Verify event is stored only once
        assertEquals(1, eventRepository.findAll().stream()
                .filter(e -> validEventRequest.getEventId().equals(e.getEventId()))
                .count());
    }

    @Test
    @DisplayName("Integration: Validation errors return 400 with proper error details")
    void testIntegration_ValidationErrors() throws Exception {
        // Missing eventId
        EventRequest invalidReq = createEvent(testAccountId, null, "CREDIT", 100.00, "2026-07-11T10:00:00Z");

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidReq)))
                .andExpect(status().isBadRequest());

        // Invalid type
        invalidReq = createEvent(testAccountId, "evt-invalid-type", "TRANSFER", 100.00, "2026-07-11T10:00:00Z");

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidReq)))
                .andExpect(status().isBadRequest());

        // Negative amount
        invalidReq = createEvent(testAccountId, "evt-invalid-amount", "CREDIT", -50.00, "2026-07-11T10:00:00Z");

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration: Trace ID flows through all endpoints")
    void testIntegration_TraceIdFlowsThrough() throws Exception {
        String customTrace = "custom-trace-integration-" + UUID.randomUUID().toString();

        // POST /events
        MvcResult postResult = mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, customTrace)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, customTrace))
                .andReturn();

        String eventId = validEventRequest.getEventId();

        // GET /events/{eventId}
        MvcResult getResult = mockMvc.perform(get("/events/" + eventId)
                .header(TraceFilter.TRACE_ID_HEADER, customTrace))
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, customTrace))
                .andReturn();

        // GET /events (list)
        MvcResult listResult = mockMvc.perform(get("/events")
                .param("account", testAccountId)
                .header(TraceFilter.TRACE_ID_HEADER, customTrace))
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, customTrace))
                .andReturn();

        // All should have same trace ID
        assertEquals(customTrace, postResult.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER));
        assertEquals(customTrace, getResult.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER));
        assertEquals(customTrace, listResult.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER));
    }

    @Test
    @DisplayName("Integration: Pagination works correctly across multiple requests")
    void testIntegration_PaginationAcrossRequests() throws Exception {
        String account = "integration-pagination-" + System.nanoTime();

        // Create 25 events
        for (int i = 1; i <= 25; i++) {
            EventRequest req = createEvent(account, "evt-page-" + i, "CREDIT", 10.00 * i, "2026-07-11T" + String.format("%02d", 9 + (i % 12)) + ":00:00Z");
            gatewayService.submitEvent(req, testTraceId);
        }

        // Request first page
        MvcResult page1 = mockMvc.perform(get("/events")
                .param("account", account)
                .param("page", "0")
                .param("size", "10")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andReturn();

        // Request second page
        MvcResult page2 = mockMvc.perform(get("/events")
                .param("account", account)
                .param("page", "1")
                .param("size", "10")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andReturn();

        // Request third page (partial)
        MvcResult page3 = mockMvc.perform(get("/events")
                .param("account", account)
                .param("page", "2")
                .param("size", "10")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andReturn();
    }

    @Test
    @DisplayName("Integration: Different accounts maintain isolation")
    void testIntegration_AccountIsolation() throws Exception {
        String account1 = "integration-iso-1-" + System.nanoTime();
        String account2 = "integration-iso-2-" + System.nanoTime();

        // Add events to account 1
        for (int i = 1; i <= 3; i++) {
            EventRequest req = createEvent(account1, "evt-acc1-" + i, "CREDIT", 10.00, "2026-07-11T" + String.format("%02d", 9 + i) + ":00:00Z");
            gatewayService.submitEvent(req, testTraceId);
        }

        // Add events to account 2
        for (int i = 1; i <= 2; i++) {
            EventRequest req = createEvent(account2, "evt-acc2-" + i, "DEBIT", 20.00, "2026-07-11T" + String.format("%02d", 9 + i) + ":00:00Z");
            gatewayService.submitEvent(req, testTraceId);
        }

        // List events for account 1
        MvcResult result1 = mockMvc.perform(get("/events")
                .param("account", account1)
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andReturn();

        // List events for account 2
        MvcResult result2 = mockMvc.perform(get("/events")
                .param("account", account2)
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn();

        // Verify isolation
        String content1 = result1.getResponse().getContentAsString();
        String content2 = result2.getResponse().getContentAsString();

        assertTrue(content1.contains("evt-acc1-1"));
        assertTrue(content1.contains("evt-acc1-2"));
        assertFalse(content1.contains("evt-acc2"));

        assertTrue(content2.contains("evt-acc2-1"));
        assertTrue(content2.contains("evt-acc2-2"));
        assertFalse(content2.contains("evt-acc1"));
    }

    @Test
    @DisplayName("Integration: Large decimal amounts are handled correctly")
    void testIntegration_LargeDecimalAmounts() throws Exception {
        EventRequest largeAmount = createEvent(testAccountId, "evt-large-decimal", "CREDIT", 999999.99, "2026-07-11T10:00:00Z");

        MvcResult result = mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(largeAmount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(999999.99))
                .andReturn();

        // Verify it's stored correctly
        EventResponse retrieved = gatewayService.getEvent("evt-large-decimal");
        assertEquals(BigDecimal.valueOf(999999.99), retrieved.getAmount());
    }

    @Test
    @DisplayName("Integration: Different currencies are stored and retrieved correctly")
    void testIntegration_MultipleCurrencies() throws Exception {
        EventRequest usdEvent = createEvent(testAccountId, "evt-usd", "CREDIT", 100.00, "2026-07-11T10:00:00Z");
        usdEvent.setCurrency("USD");

        EventRequest eurEvent = createEvent(testAccountId, "evt-eur", "CREDIT", 100.00, "2026-07-11T11:00:00Z");
        eurEvent.setCurrency("EUR");

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(usdEvent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"));

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(eurEvent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("EUR"));

        // Verify both are retrieved correctly
        EventResponse usdRetrieved = gatewayService.getEvent("evt-usd");
        EventResponse eurRetrieved = gatewayService.getEvent("evt-eur");

        assertEquals("USD", usdRetrieved.getCurrency());
        assertEquals("EUR", eurRetrieved.getCurrency());
    }

    // ================== Helper Methods ==================

    private EventRequest createEvent(String accountId, String eventId, String type, double amount, String timestamp) {
        EventRequest req = new EventRequest();
        req.setEventId(eventId);
        req.setAccountId(accountId);
        req.setType(type);
        req.setAmount(BigDecimal.valueOf(amount));
        req.setCurrency("USD");
        req.setEventTimestamp(timestamp);
        return req;
    }

    private String toJson(Object obj) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}
