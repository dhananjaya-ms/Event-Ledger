package com.dj.gateway.exception;

import com.dj.gateway.dto.EventResponse;

public class IdempotencyException extends RuntimeException {
    private final EventResponse existing;

    public IdempotencyException(EventResponse existing) {
        super("Duplicate eventId: " + (existing != null ? existing.getEventId() : ""));
        this.existing = existing;
    }

    public EventResponse getExisting() {
        return existing;
    }
}
