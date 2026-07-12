package com.dj.gateway.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
public class Event {

    @Id
    @Column(nullable = false, updatable = false)
    private String eventId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private OffsetDateTime eventTimestamp;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String metadata;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private OffsetDateTime processedAt;

    public Event() {}

    public Event(String eventId, String accountId, String type, BigDecimal amount, String currency, OffsetDateTime eventTimestamp, String metadata, String status) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
        this.metadata = metadata;
        this.status = status;
    }

    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public OffsetDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(OffsetDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
