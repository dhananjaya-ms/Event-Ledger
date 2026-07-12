package com.dj.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "EventResponse", description = "Response payload containing event details and processing status")
public class EventResponse {
    @Schema(description = "Unique identifier for the event", example = "evt-12345")
    private String eventId;

    @Schema(description = "Account ID associated with this transaction", example = "acc-67890")
    private String accountId;

    @Schema(description = "Transaction type", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"})
    private String type;

    @Schema(description = "Transaction amount", example = "100.50")
    private BigDecimal amount;

    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    private String currency;

    @Schema(description = "Event timestamp", example = "2026-07-11T10:30:00Z")
    private OffsetDateTime eventTimestamp;

    @Schema(description = "Optional metadata associated with the transaction")
    private Object metadata;

    @Schema(description = "Processing status of the event", example = "PROCESSED", allowableValues = {"PENDING", "PROCESSED", "FAILED"})
    private String status;

    @Schema(description = "Timestamp when the event was created", example = "2026-07-11T10:30:00Z")
    private OffsetDateTime createdAt;

    @Schema(description = "Timestamp when the event was processed by Account Service", example = "2026-07-11T10:30:01Z")
    private OffsetDateTime processedAt;

    public EventResponse() {}

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

    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
