package com.dj.gateway.util;

import com.dj.gateway.dto.EventRequest;
import com.dj.gateway.dto.EventResponse;
import com.dj.gateway.entity.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public class EventMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Event toEntity(EventRequest req) {
        Event e = new Event();
        e.setEventId(req.getEventId());
        e.setAccountId(req.getAccountId());
        e.setType(req.getType());
        e.setAmount(req.getAmount());
        e.setCurrency(req.getCurrency());
        try {
            e.setEventTimestamp(OffsetDateTime.parse(req.getEventTimestamp()));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("eventTimestamp must be ISO-8601");
        }
        try {
            if (req.getMetadata() != null) {
                e.setMetadata(MAPPER.writeValueAsString(req.getMetadata()));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("metadata must be serializable to JSON");
        }
        e.setStatus("PENDING");
        return e;
    }

    public static EventResponse toResponse(Event e) {
        EventResponse r = new EventResponse();
        r.setEventId(e.getEventId());
        r.setAccountId(e.getAccountId());
        r.setType(e.getType());
        r.setAmount(e.getAmount());
        r.setCurrency(e.getCurrency());
        r.setEventTimestamp(e.getEventTimestamp());
        if (e.getMetadata() != null) {
            try {
                r.setMetadata(MAPPER.readValue(e.getMetadata(), Object.class));
            } catch (JsonProcessingException ex) {
                r.setMetadata(e.getMetadata());
            }
        }
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setProcessedAt(e.getProcessedAt());
        return r;
    }
}
