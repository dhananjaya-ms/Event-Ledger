package com.dj.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Schema(name = "EventRequest", description = "Request payload for submitting a transaction event")
public class EventRequest {

    @NotBlank(message = "eventId is required")
    @Schema(description = "Unique identifier for the event", example = "evt-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventId;

    @NotBlank(message = "accountId is required")
    @Pattern(regexp = ".*\\d+.*", message = "accountId must contain at least one digit")
    @Schema(description = "Account ID associated with this transaction", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountId;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "CREDIT|DEBIT", message = "type must be CREDIT or DEBIT")
    @Schema(description = "Transaction type", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    @Schema(description = "Transaction amount", example = "100.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Schema(description = "Currency code (ISO 4217)", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @NotBlank(message = "eventTimestamp is required")
    @Schema(description = "Event timestamp in ISO 8601 format", example = "2026-07-11T10:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventTimestamp;

    @Schema(description = "Optional metadata associated with the transaction", example = "{\"reference\": \"INV-123\"}")
    private Object metadata;

    public EventRequest() {}

    // getters and setters
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

    public String getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(String eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }
}
