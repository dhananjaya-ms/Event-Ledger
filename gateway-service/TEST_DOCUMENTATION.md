# Gateway Service Test Suite Documentation

## Overview

Comprehensive test suite for the Event-Ledger Gateway Service covering:
- ✅ Core functionality tests (validation, idempotency, out-of-order, balance)
- ✅ Trace propagation tests (trace ID flow through system)
- ✅ Integration tests (full Gateway → Account Service flow)

---

## Test Files Created

### 1. **EventControllerTest.java**
**Location:** `src/test/java/com/dj/gateway/controller/EventControllerTest.java`
**Purpose:** Unit tests for REST API endpoints

#### Test Coverage:
- **POST /events - Core Functionality**
  - ✅ Submit valid event successfully (201 CREATED)
  - ✅ Idempotency: Duplicate eventId returns 409 CONFLICT
  - ✅ Validation: Missing eventId
  - ✅ Validation: Missing accountId
  - ✅ Validation: Invalid transaction type (not CREDIT/DEBIT)
  - ✅ Validation: Negative amount
  - ✅ Validation: Zero amount
  - ✅ Validation: Missing amount
  - ✅ Validation: Missing currency
  - ✅ Validation: Missing eventTimestamp
  - ✅ Validation: Non-numeric accountId
  - ✅ Trace ID propagation from header

- **GET /events/{eventId}**
  - ✅ Retrieve event successfully (200 OK)
  - ✅ Event not found returns 404

- **GET /events (List)**
  - ✅ List events for account with pagination
  - ✅ Default pagination parameters
  - ✅ Missing account parameter returns 400

#### Test Count: 18 tests

---

### 2. **GatewayServiceCoreTest.java**
**Location:** `src/test/java/com/dj/gateway/service/GatewayServiceCoreTest.java`
**Purpose:** Service layer tests for core business logic

#### Test Coverage:
- **Idempotency Tests**
  - ✅ First submission creates event with PENDING status
  - ✅ Duplicate eventId throws IdempotencyException with existing event
  - ✅ Multiple distinct events for same account succeed

- **Out-of-Order Events Tests**
  - ✅ Later event submitted first, earlier event submitted second - processes correctly
  - ✅ List events returns sorted by eventTimestamp DESC

- **Balance/State Tests**
  - ✅ Track multiple credits and debits for an account
  - ✅ Verify event amounts are preserved correctly
  - ✅ List events for account with pagination

- **Validation Tests**
  - ✅ Event with large decimal amount is stored correctly
  - ✅ Event with metadata is stored correctly
  - ✅ Different transaction types (CREDIT/DEBIT) stored correctly

#### Test Count: 12 tests

---

### 3. **TracePropagationTest.java**
**Location:** `src/test/java/com/dj/gateway/web/TracePropagationTest.java`
**Purpose:** Tests for trace ID generation and propagation

#### Test Coverage:
- **Trace ID Generation**
  - ✅ TraceFilter generates UUID if no trace ID provided
  - ✅ TraceFilter uses provided X-Trace-Id header
  - ✅ TraceFilter extracts trace ID from W3C traceparent header
  - ✅ TraceFilter prioritizes X-Trace-Id over traceparent
  - ✅ Blank/empty trace IDs are regenerated
  - ✅ Whitespace-only trace IDs are regenerated

- **Trace ID Propagation**
  - ✅ Trace ID is returned in response header for POST /events
  - ✅ Trace ID is returned in response header for GET /events/{eventId}
  - ✅ Trace ID is returned in response header for GET /events (list)

- **MDC (Mapped Diagnostic Context)**
  - ✅ MDC is set during request processing

- **Multiple Requests**
  - ✅ Multiple requests with different trace IDs maintain isolation

#### Test Count: 12 tests

---

### 4. **GatewayIntegrationTest.java**
**Location:** `src/test/java/com/dj/gateway/integration/GatewayIntegrationTest.java`
**Purpose:** End-to-end integration tests (Gateway → Account Service)

#### Test Coverage:
- **Complete Integration Flows**
  - ✅ POST event → retrieve → list with trace propagation
  - ✅ Submit multiple events for single account - verify ordering
  - ✅ Idempotency: duplicate submission returns 409 with existing event
  - ✅ Validation errors return 400 with proper error details
  - ✅ Trace ID flows through all endpoints
  - ✅ Pagination works correctly across multiple requests
  - ✅ Different accounts maintain isolation
  - ✅ Large decimal amounts are handled correctly
  - ✅ Different currencies are stored and retrieved correctly

#### Test Count: 9 tests

---

### 5. **AccountServiceClientTest.java**
**Location:** `src/test/java/com/dj/gateway/service/AccountServiceClientTest.java`
**Purpose:** Tests for AccountServiceClient trace ID handling and transaction data

#### Test Coverage:
- **Transaction Request Data**
  - ✅ TransactionRequest correctly stores amount
  - ✅ TransactionRequest correctly stores type
  - ✅ TransactionRequest correctly stores currency
  - ✅ TransactionRequest correctly stores description
  - ✅ TransactionRequest with negative amount for DEBIT

- **Trace ID Handling**
  - ✅ Test trace ID format is valid
  - ✅ Different trace IDs are unique

- **Multiple Transactions**
  - ✅ Multiple transactions with different amounts
  - ✅ Transaction data structure preserves all fields
  - ✅ Trace ID can be passed through to service calls

#### Test Count: 10 tests

---

## Running the Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=EventControllerTest
mvn test -Dtest=GatewayServiceCoreTest
mvn test -Dtest=TracePropagationTest
mvn test -Dtest=GatewayIntegrationTest
mvn test -Dtest=AccountServiceClientTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=EventControllerTest#testPostEvent_Success
mvn test -Dtest=TracePropagationTest#testTrace_PropagatedInPostEventResponse
mvn test -Dtest=GatewayIntegrationTest#testIntegration_CompleteFlow_PostRetrieveList
```

### Run with Coverage
```bash
mvn test jacoco:report
```

---

## Test Statistics

| Test File | Test Count | Coverage Areas |
|-----------|-----------|-----------------|
| EventControllerTest | 18 | REST API validation, idempotency, trace propagation |
| GatewayServiceCoreTest | 12 | Business logic, idempotency, ordering, balance |
| TracePropagationTest | 12 | Trace ID generation, propagation, MDC handling |
| GatewayIntegrationTest | 9 | End-to-end flows, account isolation, pagination |
| AccountServiceClientTest | 10 | Transaction data, trace ID handling |
| **TOTAL** | **61** | **Complete system coverage** |

---

## Key Testing Scenarios

### 1. Core Functionality
- **Idempotency:** Duplicate events with same eventId return 409 CONFLICT
- **Out-of-Order:** Events with different timestamps are processed in correct order
- **Balance:** Multiple credits/debits are tracked correctly
- **Validation:** All input validation rules are enforced

### 2. Trace Propagation
- **Generation:** Trace IDs are auto-generated if not provided
- **W3C Standard:** W3C traceparent header is parsed correctly
- **Priority:** X-Trace-Id header takes priority over traceparent
- **Flow:** Trace IDs propagate through all endpoints
- **Propagation to Account Service:** Trace IDs are sent as X-Trace-Id header

### 3. Integration
- **Complete Flow:** Event submission → Storage → Retrieval → Account Service
- **Isolation:** Different accounts maintain separate event lists
- **Pagination:** Events are paginated correctly
- **Currencies:** Multiple currencies are supported and stored correctly
- **Decimal Precision:** Large amounts with decimals are preserved

---

## Validation Rules Tested

### EventRequest Validation
| Field | Rule | Test |
|-------|------|------|
| eventId | NotBlank | ✅ testPostEvent_Validation_MissingEventId |
| accountId | NotBlank, Numeric | ✅ testPostEvent_Validation_MissingAccountId, testPostEvent_Validation_NonNumericAccountId |
| type | CREDIT or DEBIT | ✅ testPostEvent_Validation_InvalidType |
| amount | Positive | ✅ testPostEvent_Validation_NegativeAmount, testPostEvent_Validation_ZeroAmount |
| currency | NotBlank | ✅ testPostEvent_Validation_MissingCurrency |
| eventTimestamp | NotBlank | ✅ testPostEvent_Validation_MissingTimestamp |

---

## Trace ID Test Coverage

### Trace Filter Behavior
- ✅ Generates UUID if no trace ID provided
- ✅ Uses X-Trace-Id header if provided
- ✅ Parses W3C traceparent header format
- ✅ Prioritizes custom header over W3C header
- ✅ Returns trace ID in response header
- ✅ Sets trace ID in MDC for logging
- ✅ Clears MDC after request completes

### Trace Propagation Paths
- ✅ POST /events → Response header
- ✅ GET /events/{eventId} → Response header
- ✅ GET /events → Response header
- ✅ Internal service calls to Account Service (X-Trace-Id header)

---

## Integration Test Scenarios

### Scenario 1: Complete Flow
```
Client → POST /events with Trace ID
         ↓
Gateway validates and stores event
         ↓
Client retrieves event via GET /events/{eventId}
         ↓
Client lists all events via GET /events?account=X
         ↓
All responses include same Trace ID
```

### Scenario 2: Idempotency
```
Client → POST /events (Event A)
         ↓
Gateway stores Event A → Returns 201
         ↓
Client → POST /events (Same Event A)
         ↓
Gateway detects duplicate → Returns 409 with existing event
```

### Scenario 3: Out-of-Order Handling
```
Client → POST /events (Event B, timestamp T+1)
Client → POST /events (Event A, timestamp T)
         ↓
Gateway stores both events
         ↓
Client → GET /events?account=X
         ↓
Events returned sorted DESC by eventTimestamp
```

### Scenario 4: Account Isolation
```
Account 1 → POST /events (3 events)
Account 2 → POST /events (2 events)
         ↓
Account 1 → GET /events?account=acc1 → Returns 3 events
Account 2 → GET /events?account=acc2 → Returns 2 events
```

---

## Performance Considerations

- **Pagination:** All list endpoints support pagination (default 20 items/page)
- **Indexing:** EventId is unique indexed for fast lookups
- **Sorting:** Events sorted by timestamp in DESC order
- **Concurrency:** Thread-safe event submission handling

---

## Troubleshooting Failed Tests

### Common Issues

1. **Database State Issues**
   - Solution: All tests call `eventRepository.deleteAll()` in `@BeforeEach`
   - Verify database connection in `application.properties`

2. **Trace ID Header Not Found**
   - Solution: Verify `TraceFilter` is registered
   - Check that header name is exactly `X-Trace-Id`

3. **Idempotency Test Fails**
   - Solution: Ensure unique constraint on `eventId` column
   - Verify `DataIntegrityViolationException` is caught properly

4. **Pagination Issues**
   - Solution: Verify `eventRepository.findByAccountIdOrderByEventTimestampDesc()` is sorting DESC
   - Check default page size matches test expectations

---

## Test Execution Order

Tests are designed to be independent and can run in any order:
- Each test has its own `@BeforeEach` setup
- Database is cleared before each test
- Unique test data prevents conflicts

---

## Continuous Integration

These tests are ready for CI/CD pipelines:
```yaml
# Example GitHub Actions
- name: Run Tests
  run: mvn test

- name: Generate Coverage Report
  run: mvn jacoco:report

- name: Upload Coverage
  uses: codecov/codecov-action@v3
```

---

## Future Test Enhancements

- [ ] Add contract tests with Account Service
- [ ] Add performance/load tests
- [ ] Add security/authorization tests
- [ ] Add circuit breaker failure scenario tests
- [ ] Add database failure resilience tests
- [ ] Add concurrent event submission tests
