# Gateway Service - Complete Implementation

## Overview
The Gateway Service is the entry point for all client requests. It:
- Receives transaction events via REST API
- Validates input (type, amount, timestamps, required fields)
- Enforces idempotency (unique eventId)
- Stores event records in H2 database (eventdb)
- Forwards events to Account Service for processing
- Provides traceability via trace IDs
- Implements structured JSON logging

## Architecture Layers

### Controller Layer
- **EventController**: Handles `/events` endpoints (POST, GET single, GET list)
- **HealthController**: Handles `/health` endpoint
- **GlobalExceptionHandler**: Centralized error handling (409 duplicates, 400 validation, 404 not found, 500 errors)

### Service Layer  
- **GatewayService Interface**: Contract for event submission, retrieval, and listing
- **GatewayServiceImpl**: Implementation with:
  - Persist-first, process-second strategy
  - Per-account locking for concurrent processing
  - Event ordering by eventTimestamp (chronological)
  - Exactly-once delivery guarantee via unique eventId constraint
  - Status tracking (PENDING → APPLIED/FAILED)
  - WebClient integration with Account Service

### Repository Layer
- **EventRepository**: JPA repository for Event entity with custom queries:
  - `findByAccountIdOrderByEventTimestampDesc(String accountId, Pageable)`
  - `findByEventId(String eventId)`

### Data Layer (H2)
- **Event Entity**: Stores transaction events with fields:
  - eventId (unique primary key)
  - accountId (indexed)
  - type (CREDIT/DEBIT)
  - amount (BigDecimal, > 0)
  - currency
  - eventTimestamp (ISO-8601)
  - metadata (stored as JSON/CLOB)
  - status (PENDING/APPLIED/FAILED)
  - createdAt, processedAt (timestamps)

## Endpoints

### 1. POST /events
Submit a transaction event

**Request:**
```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": {
    "source": "mainframe-batch",
    "batchId": "B-9042"
  }
}
```

**Responses:**
- 201 Created: Event processed successfully
- 400 Bad Request: Validation error (missing fields, invalid amount, unknown type)
- 409 Conflict: Duplicate eventId (returns original event)

### 2. GET /events/{id}
Retrieve event by ID

**Response (200 OK):**
```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": {...},
  "status": "APPLIED",
  "createdAt": "2026-07-11T19:30:00Z",
  "processedAt": "2026-07-11T19:30:05Z"
}
```

- 404 Not Found: Event not found

### 3. GET /events?account={accountId}&page=0&size=20
List events for account (ordered by eventTimestamp DESC)

**Response (200 OK):**
```json
{
  "content": [...],
  "pageable": {...},
  "totalElements": 42,
  "totalPages": 3
}
```

### 4. GET /health
Health check

**Response (200 OK):**
```json
{
  "status": "UP"
}
```

## Idempotency & Concurrency

- **Unique Constraint**: `eventId` is a unique primary key in the database
- **Duplicate Detection**: On duplicate submission, returns original event with 409 Conflict
- **Per-Account Locking**: ConcurrentHashMap-based JVM locks prevent race conditions within single instance
- **Exactly-Once Delivery**: Events forwarded to Account Service exactly once (tracked via status column)
- **Processing Order**: Events for same account processed in ascending eventTimestamp order
- **Reconciliation**: Background Reconciler job (runs every 1 minute) re-attempts PENDING/FAILED events

## Validation Rules

All input validation is enforced at the controller level using Bean Validation:
- `eventId`: @NotBlank (required)
- `accountId`: @NotBlank (required)
- `type`: @NotBlank @Pattern("CREDIT|DEBIT") (required, enum only)
- `amount`: @NotNull @Positive (required, must be > 0)
- `currency`: @NotBlank (required)
- `eventTimestamp`: @NotBlank (required, ISO-8601 format)
- `metadata`: Optional (any JSON object)

## Tracing & Logging

- **Trace ID Generation**: TraceFilter generates/extracts trace IDs at request entry
- **MDC Propagation**: Trace ID stored in SLF4J MDC, passed to downstream services
- **Header Propagation**: X-Trace-Id header sent to Account Service
- **Structured Logging**: JSON-formatted logs via Logstash encoder including:
  - timestamp
  - level (INFO, ERROR, etc.)
  - service (gateway-service)
  - traceId
  - message
  - optional fields (accountId, eventId)

## Database Configuration

```properties
spring.datasource.url=jdbc:h2:mem:eventdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update
```

Database: In-memory H2 named "eventdb", auto-created on startup

## Running the Service

```bash
cd gateway-service
./mvnw spring-boot:run
```

Service runs on **port 8081** (account-service on 8080 to avoid conflict)

## OpenAPI / Swagger Documentation

Swagger UI available at: **http://localhost:8081/swagger-ui.html**
OpenAPI spec at: **http://localhost:8081/v3/api-docs**

## Files Overview

```
src/main/java/com/dj/gateway/
├── GatewayServiceApplication.java
├── config/
│   ├── WebClientConfig.java           # WebClient bean with trace header propagation
│   ├── GatewayRouteConfig.java        # Spring Cloud Gateway routes
│   └── OpenApiConfig.java             # Swagger/OpenAPI configuration
├── controller/
│   ├── EventController.java           # Event endpoints with OpenAPI annotations
│   ├── HealthController.java          # Health endpoint
│   └── GlobalExceptionHandler.java    # Centralized error handling
├── service/
│   ├── GatewayService.java            # Interface
│   ├── GatewayServiceImpl.java         # Implementation (persist-first, per-account locking)
│   └── AccountServiceClient.java      # WebClient calls to Account Service
├── repository/
│   └── EventRepository.java           # JPA repository
├── entity/
│   └── Event.java                     # JPA entity (unique eventId, status tracking)
├── dto/
│   ├── EventRequest.java              # Input DTO with validation
│   ├── EventResponse.java             # Output DTO
│   └── TransactionRequest.java        # Account Service request
├── exception/
│   ├── IdempotencyException.java      # Duplicate eventId
│   └── EventNotFoundException.java    # Event not found
├── web/
│   └── TraceFilter.java               # Trace ID generation/extraction
├── util/
│   └── EventMapper.java               # Entity ↔ DTO conversion
└── recon/
    └── Reconciler.java                # Background reconciliation job

src/main/resources/
├── application.properties             # H2 config, server port, gateway URL, logging
└── logback-spring.xml                 # Structured JSON logging
```

## Build & Test

```bash
# Build
./mvnw clean package

# Run tests
./mvnw test

# Run locally
./mvnw spring-boot:run

# Build docker image (optional)
docker build -t gateway-service:0.0.1 .
```

## External Integration

The gateway calls the Account Service via WebClient:
- **POST /accounts/{accountId}/transactions**: Apply event as transaction
- **Retry Logic**: Exponential backoff (configurable)
- **Error Handling**: Logs failures, marks event as FAILED after max retries
- **Trace Propagation**: Sends X-Trace-Id header for correlation

---

**Status**: ✅ Ready for deployment
**Version**: 0.0.1-SNAPSHOT
**Java**: 17
**Spring Boot**: 3.5.16
**Spring Cloud Gateway**: 2025.0.3
