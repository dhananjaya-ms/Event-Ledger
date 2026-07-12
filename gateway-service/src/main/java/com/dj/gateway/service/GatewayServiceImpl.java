package com.dj.gateway.service;



import com.dj.gateway.dto.EventRequest;
import com.dj.gateway.dto.EventResponse;
import com.dj.gateway.dto.TransactionRequest;
import com.dj.gateway.entity.Event;
import com.dj.gateway.exception.EventNotFoundException;
import com.dj.gateway.exception.IdempotencyException;
import com.dj.gateway.repository.EventRepository;
import com.dj.gateway.util.EventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GatewayServiceImpl implements GatewayService {

    private static final Logger log = LoggerFactory.getLogger(GatewayServiceImpl.class);

    private final EventRepository eventRepository;
    private final AccountServiceClient accountClient;

    // simple per-account locks
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    @Autowired
    public GatewayServiceImpl(EventRepository eventRepository, AccountServiceClient accountClient) {
        this.eventRepository = eventRepository;
        this.accountClient = accountClient;
    }

    @Override
    @Transactional
    public EventResponse submitEvent(EventRequest req, String traceId) {
        // convert to entity
        Event e = EventMapper.toEntity(req);
        try {
            Event saved = eventRepository.save(e);
            log.info("Persisted event {} for account {}", saved.getEventId(), saved.getAccountId());
        } catch (DataIntegrityViolationException ex) {
            // duplicate key - fetch existing and return
        	System.out.println("Duplicate eventId detected: " + e.getEventId());
            Event existing = eventRepository.findByEventId(e.getEventId()).orElse(null);
            EventResponse resp = existing == null ? null : EventMapper.toResponse(existing);
            throw new IdempotencyException(resp);
        }

        // process pending events for the account (ordered by eventTimestamp asc)
        processPendingForAccount(e.getAccountId(), traceId);

        Event out = eventRepository.findByEventId(e.getEventId()).orElseThrow(() -> new EventNotFoundException(e.getEventId()));
        return EventMapper.toResponse(out);
    }

    private void processPendingForAccount(String accountId, String traceId) {
        Object lock = locks.computeIfAbsent(accountId, k -> new Object());
        synchronized (lock) {
            List<Event> pending = eventRepository.findByAccountIdOrderByEventTimestampDesc(accountId, Pageable.unpaged())
                    .stream()
                    .filter(ev -> "PENDING".equals(ev.getStatus()) || "FAILED".equals(ev.getStatus()))
                    .sorted(Comparator.comparing(Event::getEventTimestamp))
                    .toList();

            for (Event ev : pending) {
                try {
                    // map to transaction request
                    TransactionRequest tx = toTransactionRequest(ev);
                    accountClient.applyTransaction(ev.getAccountId(), tx, traceId);
                    ev.setStatus("APPLIED");
                    ev.setProcessedAt(OffsetDateTime.now());
                    eventRepository.save(ev);
                    log.info("Applied event {}", ev.getEventId());
                } catch (Exception ex) {
                    log.error("Failed to apply event {}: {}", ev.getEventId(), ex.getMessage());
                    ev.setStatus("FAILED");
                    ev.setProcessedAt(OffsetDateTime.now());
                    eventRepository.save(ev);
                }
            }
        }
    }

    private TransactionRequest toTransactionRequest(Event ev) {
        java.math.BigDecimal amount = ev.getAmount();
        if ("DEBIT".equals(ev.getType())) {
            amount = amount.negate();
        }
        String desc = "event:" + ev.getEventId();
        return new TransactionRequest(amount, desc);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEvent(String eventId) {
        Event ev = eventRepository.findByEventId(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        return EventMapper.toResponse(ev);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> listEventsForAccount(String accountId, Pageable pageable) {
        Page<Event> page = eventRepository.findByAccountIdOrderByEventTimestampDesc(accountId, pageable);
        return page.map(EventMapper::toResponse);
    }

	
}
