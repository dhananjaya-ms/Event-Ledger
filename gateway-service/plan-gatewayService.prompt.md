# Gateway Service Implementation Plan

## Overview

Build a Spring Boot gateway that accepts transaction events, validates and persists them to an H2 in-memory DB named `eventdb`, enforces idempotency using a unique `eventId` constraint, and forwards accepted events to the Account Service. Use a controller → service → repository structure, store metadata as JSON, and call the Account Service with a configurable WebClient.

## Event Payload Reference

Submitted to `POST /events` on the Gateway:

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

## Event Schema (H2 Database: eventdb)

| Field | Type | Required | Notes |
|---|---|---|---|
| `eventId` | string | Yes | Unique identifier for the event |
| `accountId` | string | Yes | The account this event belongs to |
| `type` | string | Yes | Must be `"CREDIT"` or `"DEBIT"` |
| `amount` | number | Yes | Must be greater than 0 |
| `currency` | string | Yes | e.g., `"USD"` |
| `eventTimestamp` | string (ISO 8601) | Yes | When the event originally occurred |
| `metadata` | object | No | Optional additional context |
| `status` | string | No | `PENDING`, `APPLIED`, `FAILED` (internal tracking) |
| `createdAt` | timestamp | No | When record was inserted |
| `processedAt` | timestamp | No | When event was forwarded/processed |

## Required Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/events` | Submit a transaction event |
| `GET` | `/events/{id}` | Retrieve a single event by its ID |
| `GET` | `/events?account={accountId}` | List events for an account, ordered by event timestamp |
| `GET` | `/health` | Health check |

## Implementation Steps

### Step 1: Configuration and H2 Setup

**File: `src/main/resources/application.properties`**

- Configure H2 URL: `jdbc:h2:mem:eventdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Driver: `org.h2.Driver`
- Username: `sa` (blank password)
- Set `spring.jpa.hibernate.ddl-auto=update` to auto-create schema
- Enable H2 console at `/h2-console` via `spring.h2.console.enabled=true`
- Set `spring.jpa.show-sql=true` and `spring.jpa.properties.hibernate.format_sql=true` for debug logging
- Add custom property `gateway.account-service.url` (e.g., `http://localhost:8080`) for Account Service base URL
- Add `server.port=8081` to avoid conflict with account-service on 8080

### Step 2: JPA Entity

**File: `src/main/java/com/dj/gateway/entity/Event.java`**

- Class with fields:
  - `eventId` (String) — primary key, unique constraint
  - `accountId` (String) — indexed
  - `type` (String enum or String) — CREDIT or DEBIT
  - `amount` (BigDecimal) — must be > 0
  - `currency` (String)
  - `eventTimestamp` (LocalDateTime or ZonedDateTime) — ISO 8601 from payload
  - `metadata` (String @Lob or @Column with JSON type) — store as JSON string
  - `status` (String enum or String) — PENDING, APPLIED, FAILED
  - `createdAt` (LocalDateTime) — @CreationTimestamp
  - `processedAt` (LocalDateTime) — updated after forward attempt
- Add JPA annotations: `@Entity`, `@Table(name = "events")`, `@Id` on `eventId`, `@Column(unique=true)` on `eventId`
- Add `@UniqueConstraint(columnNames = "eventId")` at table level for safety
- Constructors: default, full-arg, partial-arg (for create with defaults)
- Getters/setters for all fields

### Step 3: Repository

**File: `src/main/java/com/dj/gateway/repository/EventRepository.java`**

- Interface extending `JpaRepository<Event, String>`
- Methods:
  - `Page<Event> findByAccountIdOrderByEventTimestampDesc(String accountId, Pageable pageable)`
  - `Optional<Event> findByEventId(String eventId)` (inherited, but can override)
  - `Optional<Event> findByAccountIdAndEventId(String accountId, String eventId)` (optional, for combined lookup)

### Step 4: DTOs and Mappers

**Files:**
- `src/main/java/com/dj/gateway/dto/EventRequest.java` — input DTO matching JSON payload (eventId, accountId, type, amount, currency, eventTimestamp, metadata)
- `src/main/java/com/dj/gateway/dto/EventResponse.java` — output DTO (eventId, accountId, type, amount, currency, eventTimestamp, metadata, status, createdAt, processedAt)
- `src/main/java/com/dj/gateway/util/EventMapper.java` — simple converter methods `toEntity(EventRequest)`, `toResponse(Event)`

**Validation on EventRequest:**
- `@NotBlank` on eventId, accountId, type, currency
- `@NotNull` on amount, eventTimestamp
- `@Positive` on amount
- `@Pattern(regexp = "CREDIT|DEBIT")` on type (or use enum)

### Step 5: Service Layer

**File: `src/main/java/com/dj/gateway/service/GatewayService.java`**

Interface with methods:
```java
public interface GatewayService {
    EventResponse submitEvent(EventRequest eventRequest) throws IdempotencyException;
    EventResponse getEvent(String eventId);
    Page<EventResponse> listEventsForAccount(String accountId, Pageable pageable);
}
```

**File: `src/main/java/com/dj/gateway/service/GatewayServiceImpl.java`**

Implement interface:

- **submitEvent(EventRequest)**:
  - Validate input (via controller @Valid annotation)
  - Try to persist Event with status=PENDING
  - Catch `DataIntegrityViolationException` on duplicate eventId → retrieve existing event, return it with 409 status (or wrap in custom exception `IdempotencyException` for controller to handle)
  - On successful persist:
    - Call Account Service async or sync to apply transaction (POST `/accounts/{accountId}/transactions`)
    - Catch HTTP errors; on success update status=APPLIED and processedAt=now(); on failure update status=FAILED
    - Return response
  - Use `@Transactional` annotation

- **getEvent(String eventId)**:
  - Query repository `findByEventId(eventId)`
  - Return mapped response or throw custom exception (which controller maps to 404)

- **listEventsForAccount(String accountId, Pageable)**:
  - Query repository with pageable
  - Return mapped page

### Step 6: WebClient for Account Service Integration

**File: `src/main/java/com/dj/gateway/config/WebClientConfig.java`**

- Create a `@Configuration` class
- Expose a `@Bean` WebClient (or RestTemplate) configured with:
  - Base URL from property `gateway.account-service.url`
  - Timeout and retry policies
  - Example:
    ```java
    @Bean
    public WebClient webClient(WebClient.Builder builder, @Value("${gateway.account-service.url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
    ```

### Step 7: Account Service Client

**File: `src/main/java/com/dj/gateway/service/AccountServiceClient.java`**

- Service class that wraps WebClient calls to Account Service
- Method `applyTransaction(String accountId, TransactionRequest txn)` → POST to `/accounts/{accountId}/transactions`
- Handle HTTP 201, 4xx, 5xx responses; throw custom exception on failure
- Implement retry logic (Spring Retry `@Retryable` or Reactor `retryWhen`)

**File: `src/main/java/com/dj/gateway/dto/TransactionRequest.java`**

- DTO matching Account Service input: `amount`, `description`
- Created from Event by mapping `type` + `amount` to a signed amount (CREDIT = positive, DEBIT = negative) or keep separate and let Account Service know direction

### Step 8: Exception Handling

**File: `src/main/java/com/dj/gateway/exception/IdempotencyException.java`**

- Custom exception for duplicate eventId
- Include existing event details

**File: `src/main/java/com/dj/gateway/exception/EventNotFoundException.java`**

- Thrown when event not found by ID

**File: `src/main/java/com/dj/gateway/controller/GlobalExceptionHandler.java`** (Optional but recommended)

- `@ControllerAdvice` class
- `@ExceptionHandler` methods for IdempotencyException → 409, EventNotFoundException → 404, validation errors → 400, etc.

### Step 9: Controller

**File: `src/main/java/com/dj/gateway/controller/EventController.java`**

- `@RestController` on class
- `@RequestMapping("/events")`

Methods:
- **POST /events**:
  - `@PostMapping`, accept `@RequestBody @Valid EventRequest eventRequest`
  - Call `gatewayService.submitEvent(eventRequest)`
  - Return 201 with response body
  - Handle IdempotencyException: return 409 Conflict with existing event

- **GET /events/{id}**:
  - `@GetMapping("/{eventId}")`
  - Call `gatewayService.getEvent(eventId)`
  - Return 200 or 404

- **GET /events?account={accountId}**:
  - `@GetMapping`
  - `@RequestParam(name = "account") String accountId`
  - `@RequestParam(defaultValue = "0") int page`
  - `@RequestParam(defaultValue = "20") int size`
  - Call `gatewayService.listEventsForAccount(accountId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventTimestamp")))`
  - Return 200 with Page

- **GET /health**:
  - Simple health check, return `{ "status": "UP" }`

### Step 10: POM Configuration

**File: `pom.xml`**

Ensure dependencies:
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-web`
- `spring-boot-starter-webflux` (for WebClient)
- `spring-boot-starter-validation`
- `spring-boot-devtools`
- `h2` (runtime scope)
- `lombok` (optional)
- `spring-boot-starter-test` (test scope)

---

## Implementation Order

1. Update `application.properties` with H2 and custom property
2. Create `Event` entity
3. Create `EventRepository`
4. Create DTOs (EventRequest, EventResponse, TransactionRequest)
5. Create EventMapper utility
6. Create exceptions (IdempotencyException, EventNotFoundException)
7. Create AccountServiceClient
8. Create WebClientConfig
9. Create GatewayService interface and GatewayServiceImpl
10. Create EventController
11. Create GlobalExceptionHandler (optional)
12. Verify POM has required dependencies
13. Run `mvn clean package` and `mvn spring-boot:run` to test

---

## Design Decisions

### Idempotency Enforcement
- **Mechanism**: Unique database constraint on `eventId` column
- **Conflict Handling**: On duplicate, catch `DataIntegrityViolationException` in service, retrieve and return existing event (HTTP 409 Conflict or 200 OK depending on policy)
- **Advantage**: Simple, single-instance reliable, no external state needed
- **Limitation**: Works best in single instance; for distributed gateway instances, consider Redis-backed idempotency store

Additional requirements (duplicate submissions and ordering):

- Behavior on duplicate submissions: Submitting the same `eventId` more than once MUST NOT create a duplicate event or alter the account balance. On a duplicate submission the Gateway will return the original stored event and an appropriate HTTP status code (409 Conflict is recommended). The Gateway must not re-forward a duplicate event to the Account Service.

- Out-of-order arrivals: Events may arrive out of chronological order. Event listings must be returned ordered by `eventTimestamp` (chronological). To ensure balances are correct regardless of arrival order, the Gateway will treat events as ledger entries (amounts are additive/subtractive) and ensure each unique event is processed exactly once. Processing strategy (detailed below) will persist events immediately and then deliver pending events for a given account to the Account Service in timestamp order; already-applied events will not be re-applied.


### Metadata Storage
- **Format**: Store as JSON String in `@Lob` column
- **Parsing**: Parse on retrieval if needed; for now, treat as opaque string
- **Advantage**: Flexible, no schema constraints on metadata
- **Alternative**: Use `@Convert` with Jackson/Hibernate JSON type

### Forwarding to Account Service
- **Strategy**: Synchronous call in the same transaction (or separate transaction)
- **Retry**: Implement exponential backoff with Spring Retry or manual retry loop
- **Status Updates**: Mark PENDING → APPLIED on success, PENDING → FAILED on max retries
- **Failure Mode**: If Account Service is unavailable, store as FAILED and return 5xx or log for manual retry
- **Advantage**: Immediate feedback to caller, simpler to test
- **Trade-off**: Blocking call; for high throughput, consider async queue (Kafka) later

Processing and ordering details:

- Persist-first, process-second: On `POST /events` the Gateway persists the event record (status=PENDING) as the first step. After persistence the Gateway will attempt to process PENDING events for the same account in strict ascending `eventTimestamp` order. This guarantees that processing of events for an account is deterministic and chronological even when arrivals are out-of-order.

- Exactly-once per event: The combination of the unique `eventId` constraint and a status column (`PENDING`, `APPLIED`, `FAILED`) ensures an event is forwarded to the Account Service at most once. If a duplicate submission is detected during initial insert, the service returns the original event and skips forwarding.

- Concurrency control: Implement a per-account processing lock (a database lock row, JVM ConcurrentHashMap-based lock, or a serialized Executor per account) to ensure that only a single processing worker handles pending events for a given account at a time. This prevents races when multiple events for the same account arrive concurrently.

- Reconciliation / recovery: On service startup or on failure paths, a background reconciler job should scan for PENDING/FAILED events and re-attempt processing in timestamp order.


### Event Timestamp
- **Storage**: Store `eventTimestamp` from payload as-is (ISO 8601 string converted to LocalDateTime or ZonedDateTime)
- **Ordering**: Query results ordered by `eventTimestamp DESC` from payload, not system time
- **Use Case**: Reflects when the event truly occurred (e.g., mainframe batch timestamp), not when gateway received it

### Validation and error responses

- The Gateway must reject events with missing required fields, negative/zero amounts, or unknown event types. Use Bean Validation annotations on the `EventRequest` DTO and validate `type` against an explicit enum or regex.
- For invalid requests return HTTP 400 Bad Request with a JSON body that contains a clear error message and list of validation failures. Example:

  {
    "status": 400,
    "error": "Bad Request",
    "message": "amount must be greater than 0",
    "details": ["amount: must be greater than 0"]
  }

### Tracing and structured logging

- Objective: Generate a traceable path for a client request across Gateway → Account Service and include trace identifiers in logs. OpenTelemetry is preferred.

- Trace generation and propagation:
  - The Gateway generates a trace ID at the start of request processing if none is present (accept incoming `traceparent` header when provided).
  - The trace ID is added to the logging context (MDC) and included in structured logs.
  - When the Gateway calls the Account Service, it forwards the trace context using the W3C `traceparent` header (and `tracestate` if available). If not using full OpenTelemetry instrumentation, at minimum propagate a custom header `X-Trace-Id` or the W3C `traceparent` carrying the generated trace identifier.
  - The Account Service must log the received trace ID in its structured logs.

- OpenTelemetry integration:
  - Add `io.opentelemetry:opentelemetry-sdk` and the Spring/OTel auto-instrumentation dependencies to the POM.
  - Instrument the WebClient (or RestTemplate) used to call Account Service to propagate trace context automatically (OpenTelemetry WebClient instrumentation or manual header propagation).
  - Optionally export traces to a collector (OTel Collector, Jaeger) configured via properties.

- Structured logging:
  - Use a JSON log encoder (Logstash Logback Encoder or built-in Logback JSON encoder) and configure logback to produce JSON logs including at least: `timestamp`, `level`, `service` (gateway-service), `traceId`, `spanId` (if available), `message`, and optional `fields` (accountId, eventId).
  - Ensure both Gateway and Account Service use the same structured logging format so a single trace can be correlated across services.

### Implementation checklist additions

- Enforce duplicate behavior: return original event, do not forward duplicates.
- Persist-first, then process events per-account in `eventTimestamp` order.
- Reject invalid requests with descriptive 400 responses.
- Integrate OpenTelemetry and propagate trace context to Account Service; log `traceId` in structured JSON logs.

---

## Testing Strategy (Optional)

- Unit tests for GatewayService with mocked repository and WebClient
- Integration test with TestContainers or embedded H2
- MockWebServer for Account Service stub
- Example: `@SpringBootTest` with `@AutoConfigureWebTestClient` testing POST /events → verify stored and Account Service called

---

## Future Enhancements

1. **Asynchronous Forwarding**: Replace synchronous Account Service calls with Kafka producer → Consumer pattern for resilience
2. **Distributed Idempotency**: Add Redis-backed idempotency store for multi-instance deployments
3. **Event Audit Log**: Track submission, processing attempts, outcomes separately
4. **Metrics & Monitoring**: Counter metrics for submitted/applied/failed events, latency histograms
5. **Pagination & Filtering**: Add more query endpoints (filter by type, date range, status)
6. **Rate Limiting**: Add Spring Cloud Gateway or custom interceptor for client rate limiting
7. **Event Replay**: Mechanism to replay failed events (manual or automatic retry jobs)
