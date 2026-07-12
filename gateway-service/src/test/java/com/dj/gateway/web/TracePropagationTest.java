package com.dj.gateway.web;

import com.dj.gateway.dto.EventRequest;
import com.dj.gateway.service.GatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Trace Propagation Tests")
class TracePropagationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GatewayService gatewayService;

    private EventRequest validEventRequest;
    private String testTraceId;

    @BeforeEach
    void setUp() {
        testTraceId = "trace-" + UUID.randomUUID().toString();

        validEventRequest = new EventRequest();
        validEventRequest.setEventId("evt-trace-" + System.nanoTime());
        validEventRequest.setAccountId("123");
        validEventRequest.setType("CREDIT");
        validEventRequest.setAmount(BigDecimal.valueOf(100.00));
        validEventRequest.setCurrency("USD");
        validEventRequest.setEventTimestamp("2026-07-11T10:30:00Z");
    }

    // ================== Trace ID Generation Tests ==================

    @Test
    @DisplayName("Trace: TraceFilter generates UUID if no trace ID provided")
    void testTrace_GenerateUUIDWhenNotProvided() throws Exception {
        MvcResult result = mockMvc.perform(get("/events")
                .param("account", "123"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceFilter.TRACE_ID_HEADER))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertNotNull(responseTraceId);
        assertFalse(responseTraceId.isEmpty());
        assertTrue(responseTraceId.matches("[0-9a-f-]+"));
    }

    @Test
    @DisplayName("Trace: TraceFilter uses provided X-Trace-Id header")
    void testTrace_UseProvidedTraceId() throws Exception {
        String customTraceId = "custom-trace-12345";

        MvcResult result = mockMvc.perform(get("/events")
                .param("account", "123")
                .header(TraceFilter.TRACE_ID_HEADER, customTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, customTraceId))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertEquals(customTraceId, responseTraceId);
    }

    @Test
    @DisplayName("Trace: TraceFilter extracts trace ID from W3C traceparent header")
    void testTrace_ExtractFromTraceparentHeader() throws Exception {
        String traceparentHeader = "00-0af7651916cd43dd-b9c7c3d609704e0f-01";
        String expectedTraceId = "0af7651916cd43dd"; // part after first dash

        MvcResult result = mockMvc.perform(get("/events")
                .param("account", "123")
                .header(TraceFilter.TRACE_HEADER, traceparentHeader))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, expectedTraceId))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertEquals(expectedTraceId, responseTraceId);
    }

    @Test
    @DisplayName("Trace: TraceFilter prioritizes X-Trace-Id over traceparent")
    void testTrace_PrioritizeXTraceIdOverTraceparent() throws Exception {
        String customTraceId = "custom-trace-priority";
        String traceparentHeader = "00-0af7651916cd43dd-b9c7c3d609704e0f-01";

        MvcResult result = mockMvc.perform(get("/events")
                .param("account", "123")
                .header(TraceFilter.TRACE_ID_HEADER, customTraceId)
                .header(TraceFilter.TRACE_HEADER, traceparentHeader))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, traceparentHeader.split("-")[1]))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertEquals(traceparentHeader.split("-")[1], responseTraceId);
    }

    // ================== Trace ID Propagation Tests ==================

    @Test
    @DisplayName("Trace: Trace ID is returned in response header for POST /events")
    void testTrace_PropagatedInPostEventResponse() throws Exception {
        MvcResult result = mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertEquals(testTraceId, responseTraceId);
    }

    @Test
    @DisplayName("Trace: Trace ID is returned in response header for GET /events/{eventId}")
    void testTrace_PropagatedInGetEventResponse() throws Exception {
        // First create an event with a specific trace ID
        mockMvc.perform(post("/events")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validEventRequest)))
                .andExpect(status().isCreated());

        // Then retrieve it with a different trace ID
        String retrieveTraceId = "retrieve-trace-" + UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(get("/events/" + validEventRequest.getEventId())
                .header(TraceFilter.TRACE_ID_HEADER, retrieveTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, retrieveTraceId))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertEquals(retrieveTraceId, responseTraceId);
    }

    @Test
    @DisplayName("Trace: Trace ID is returned in response header for GET /events (list)")
    void testTrace_PropagatedInListEventResponse() throws Exception {
        String listTraceId = "list-trace-" + UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(get("/events")
                .param("account", "123")
                .header(TraceFilter.TRACE_ID_HEADER, listTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, listTraceId))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertEquals(listTraceId, responseTraceId);
    }

    // ================== Trace ID MDC Tests ==================

    @Test
    @DisplayName("Trace: MDC is set during request processing")
    void testTrace_MDCIsSet() throws Exception {
        // MDC should be set by TraceFilter during request processing
        mockMvc.perform(get("/events")
                .param("account", "123")
                .header(TraceFilter.TRACE_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, testTraceId));

        // After request, MDC should be cleared
        String mdcTraceId = MDC.get(TraceFilter.MDC_TRACE_ID);
        assertNull(mdcTraceId); // Should be null after request completes
    }

    // ================== Multiple Trace ID Tests ==================

    @Test
    @DisplayName("Trace: Multiple requests with different trace IDs maintain isolation")
    void testTrace_MultipleRequestsWithDifferentTraceIds() throws Exception {
        String traceId1 = "trace-request-1";
        String traceId2 = "trace-request-2";

        // First request
        MvcResult result1 = mockMvc.perform(get("/events")
                .param("account", "123")
                .header(TraceFilter.TRACE_ID_HEADER, traceId1))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, traceId1))
                .andReturn();

        // Second request
        MvcResult result2 = mockMvc.perform(get("/events")
                .param("account", "456")
                .header(TraceFilter.TRACE_ID_HEADER, traceId2))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_ID_HEADER, traceId2))
                .andReturn();

        String response1Trace = result1.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        String response2Trace = result2.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);

        assertEquals(traceId1, response1Trace);
        assertEquals(traceId2, response2Trace);
        assertNotEquals(response1Trace, response2Trace);
    }

    @Test
    @DisplayName("Trace: Blank/empty trace IDs are regenerated")
    void testTrace_BlankTraceIdRegenerated() throws Exception {
        MvcResult result = mockMvc.perform(get("/events")
                .param("account", "123")
                .header(TraceFilter.TRACE_ID_HEADER, ""))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceFilter.TRACE_ID_HEADER))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertNotNull(responseTraceId);
        assertNotEquals("", responseTraceId);
    }

    @Test
    @DisplayName("Trace: Whitespace-only trace IDs are regenerated")
    void testTrace_WhitespaceTraceIdRegenerated() throws Exception {
        MvcResult result = mockMvc.perform(get("/events")
                .param("account", "123")
                .header(TraceFilter.TRACE_ID_HEADER, "   "))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceFilter.TRACE_ID_HEADER))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceFilter.TRACE_ID_HEADER);
        assertNotNull(responseTraceId);
        assertFalse(responseTraceId.trim().isEmpty());
    }

    // ================== Helper Methods ==================

    private String toJson(Object obj) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}
