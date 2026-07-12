package com.dj.gateway.repository;

import com.dj.gateway.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {
    Page<Event> findByAccountIdOrderByEventTimestampDesc(String accountId, Pageable pageable);
    Optional<Event> findByEventId(String eventId);
    Optional<Event> findByAccountIdAndEventId(String accountId, String eventId);
}
