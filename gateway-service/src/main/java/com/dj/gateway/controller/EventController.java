package com.dj.gateway.controller;

import com.dj.gateway.dto.EventRequest;
import com.dj.gateway.dto.EventResponse;
import com.dj.gateway.exception.IdempotencyException;
import com.dj.gateway.service.GatewayService;
import com.dj.gateway.web.TraceFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
@Validated
@Tag(name = "Events", description = "Transaction event management endpoints")
public class EventController {

    private final GatewayService gatewayService;

    @Autowired
    public EventController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping
    @Operation(summary = "Submit a transaction event", description = "Accepts a transaction event, validates input, persists to database, and forwards to Account Service.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created and forwarded successfully", content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error - missing fields, invalid amount, or unknown type"),
            @ApiResponse(responseCode = "409", description = "Duplicate eventId - original event returned", content = @Content(schema = @Schema(implementation = EventResponse.class)))
    })
    public ResponseEntity<?> postEvent(@RequestHeader(value = TraceFilter.TRACE_ID_HEADER, required = false) String traceId,
                                       @Valid @RequestBody EventRequest req) {
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get(TraceFilter.MDC_TRACE_ID);
        }
        try {
            EventResponse resp = gatewayService.submitEvent(req, traceId);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (IdempotencyException ie) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ie.getExisting());
        }
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Retrieve a single event by ID", description = "Returns event details including status and timestamps.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event found", content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<EventResponse> getEvent(
            @Parameter(description = "Event ID", required = true)
            @PathVariable String eventId) {
        EventResponse resp = gatewayService.getEvent(eventId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    @Operation(summary = "List events for an account", description = "Returns events ordered by eventTimestamp DESC with pagination support.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid account ID or pagination parameters")
    })
    public ResponseEntity<Page<EventResponse>> listEvents(
            @Parameter(description = "Account ID (required)", required = true)
            @RequestParam(name = "account") String accountId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size);
        Page<EventResponse> result = gatewayService.listEventsForAccount(accountId, p);
        return ResponseEntity.ok(result);
    }
}
