package com.dj.gateway.controller;

import com.dj.gateway.dto.EventRequest;
import com.dj.gateway.dto.EventResponse;
import com.dj.gateway.exception.IdempotencyException;
import com.dj.gateway.service.GatewayService;
import com.dj.gateway.web.TraceFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("EventController Tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayService gatewayService;

    private EventRequest validEventRequest;
    private EventResponse eventResponse;
    private String validTraceId;

    @BeforeEach
    void setUp() {
        validTraceId = "test-trace-12345";

        validEventRequest = new EventRequest();
        validEventRequest.setEventId("evt-001");
        validEventRequest.setAccountId("123");
        validEventRequest.setType("CREDIT");
        validEventRequest.setAmount(BigDecimal.valueOf(100.00));
        validEventRequest.setCurrency("USD");
        validEventRequest.setEventTimestamp("2026-07-11T10:30:00Z");

        eventResponse = new EventResponse();
        eventResponse.setEventId("evt-001");
        eventResponse.setAccountId("123");
        eventResponse.setType("CREDIT");
        eventResponse.setAmount(BigDecimal.valueOf(100.00));
        eventResponse.setCurrency("USD");
        eventResponse.setStatus("PROCESSED");
        eventResponse.setCreatedAt(OffsetDateTime.now());
        eventResponse.setProcessedAt(OffsetDateTime.now());
    }

    // ================== POST /events - Core Functionality Tests ==================

    @Test
    @DisplayName("POST /events - Submit valid event successfully")
    void testPostEvent_Success() throws Exception {
        when(gatewayService.submitEvent(any(EventRequest.class), anyString()))
                .thenReturn(eventResponse);

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, validTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    @DisplayName("POST /events - Idempotency: Duplicate eventId returns 409 CONFLICT")
    void testPostEvent_Idempotency_DuplicateEventId() throws Exception {
        when(gatewayService.submitEvent(any(EventRequest.class), anyString()))
                .thenThrow(new IdempotencyException(eventResponse));

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.eventId").value("evt-001"));
    }

    @Test
    @DisplayName("POST /events - Validation: Missing eventId")
    void testPostEvent_Validation_MissingEventId() throws Exception {
        validEventRequest.setEventId(null);

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /events - Validation: Missing accountId")
    void testPostEvent_Validation_MissingAccountId() throws Exception {
        validEventRequest.setAccountId(null);

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - Validation: Invalid transaction type (not CREDIT/DEBIT)")
    void testPostEvent_Validation_InvalidType() throws Exception {
        validEventRequest.setType("TRANSFER");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - Validation: Negative amount")
    void testPostEvent_Validation_NegativeAmount() throws Exception {
        validEventRequest.setAmount(BigDecimal.valueOf(-50.00));

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - Validation: Zero amount")
    void testPostEvent_Validation_ZeroAmount() throws Exception {
        validEventRequest.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - Validation: Missing amount")
    void testPostEvent_Validation_MissingAmount() throws Exception {
        validEventRequest.setAmount(null);

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - Validation: Missing currency")
    void testPostEvent_Validation_MissingCurrency() throws Exception {
        validEventRequest.setCurrency(null);

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - Validation: Missing eventTimestamp")
    void testPostEvent_Validation_MissingTimestamp() throws Exception {
        validEventRequest.setEventTimestamp(null);

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - Validation: Non-numeric accountId")
    void testPostEvent_Validation_NonNumericAccountId() throws Exception {
        validEventRequest.setAccountId("acc-abc");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events - Trace ID propagation from header")
    void testPostEvent_TraceIdPropagation() throws Exception {
        when(gatewayService.submitEvent(any(EventRequest.class), eq(validTraceId)))
                .thenReturn(eventResponse);

        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, validTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(TraceFilter.TRACE_ID_HEADER));
    }

    // ================== GET /events/{eventId} Tests ==================

    @Test
    @DisplayName("GET /events/{eventId} - Retrieve event successfully")
    void testGetEvent_Success() throws Exception {
        when(gatewayService.getEvent("evt-001"))
                .thenReturn(eventResponse);

        mockMvc.perform(get("/events/evt-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.accountId").value("123"));
    }

    @Test
    @DisplayName("GET /events/{eventId} - Event not found returns 404")
    void testGetEvent_NotFound() throws Exception {
        when(gatewayService.getEvent("evt-999"))
                .thenThrow(new com.dj.gateway.exception.EventNotFoundException("evt-999"));

        mockMvc.perform(get("/events/evt-999"))
                .andExpect(status().isNotFound());
    }

    // ================== GET /events (List) Tests ==================

    @Test
    @DisplayName("GET /events - List events for account with pagination")
    void testListEvents_Success() throws Exception {
        Page<EventResponse> page = new PageImpl<>(List.of(eventResponse), PageRequest.of(0, 20), 1);

        when(gatewayService.listEventsForAccount(eq("123"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/events")
                .param("account", "123")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value("evt-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /events - List events with default pagination")
    void testListEvents_DefaultPagination() throws Exception {
        Page<EventResponse> page = new PageImpl<>(List.of(eventResponse), PageRequest.of(0, 20), 1);

        when(gatewayService.listEventsForAccount(eq("123"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/events")
                .param("account", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageSize").value(20));
    }

    @Test
    @DisplayName("GET /events - Missing account parameter returns 400")
    void testListEvents_MissingAccountParameter() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isBadRequest());
    }

    // Helper method to convert object to JSON string
    private String toJson(Object obj) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}
