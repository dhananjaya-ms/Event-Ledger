package com.dj.gateway.service;

import com.dj.gateway.dto.EventRequest;
import com.dj.gateway.dto.EventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GatewayService {
    EventResponse submitEvent(EventRequest req, String traceId);
    EventResponse getEvent(String eventId);
    Page<EventResponse> listEventsForAccount(String accountId, Pageable pageable);
}
