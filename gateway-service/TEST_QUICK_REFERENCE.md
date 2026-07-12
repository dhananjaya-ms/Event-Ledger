# Test Suite Quick Reference

## Summary

✅ **61 comprehensive tests** covering:
- Core functionality (validation, idempotency, out-of-order, balance)
- Trace propagation (trace IDs flowing through system)
- Integration (full Gateway ↔ Account Service flows)

---

## Quick Start

### Run All Tests
```bash
cd gateway-service
mvn test
```

### Run Specific Test Suite
```bash
# REST API tests
mvn test -Dtest=EventControllerTest

# Business logic tests
mvn test -Dtest=GatewayServiceCoreTest

# Trace propagation tests
mvn test -Dtest=TracePropagationTest

# Integration tests
mvn test -Dtest=GatewayIntegrationTest

# Transaction handling tests
mvn test -Dtest=AccountServiceClientTest
```

### Run Single Test
```bash
mvn test -Dtest=EventControllerTest#testPostEvent_Success
mvn test -Dtest=TracePropagationTest#testTrace_PropagatedInPostEventResponse
mvn test -Dtest=GatewayIntegrationTest#testIntegration_CompleteFlow_PostRetrieveList
```

---

## Test Files & Line Counts

| File | Location | Tests | Focus |
|------|----------|-------|-------|
| **EventControllerTest** | `src/test/java/com/dj/gateway/controller/` | 18 | REST API validation, idempotency, trace IDs |
| **GatewayServiceCoreTest** | `src/test/java/com/dj/gateway/service/` | 12 | Business logic, ordering, balance |
| **TracePropagationTest** | `src/test/java/com/dj/gateway/web/` | 12 | Trace ID generation & propagation |
| **GatewayIntegrationTest** | `src/test/java/com/dj/gateway/integration/` | 9 | End-to-end workflows |
| **AccountServiceClientTest** | `src/test/java/com/dj/gateway/service/` | 10 | Transaction data & trace handling |

---

## Test Categories at a Glance

### ✅ Core Functionality Tests (34 tests)

**Idempotency**
- Duplicate eventId returns 409 CONFLICT ✅
- Existing event is returned with duplicate ✅
- Multiple distinct events succeed ✅

**Validation**
- Missing required fields (eventId, accountId, type, amount, currency, timestamp) ✅
- Invalid values (type not CREDIT/DEBIT, negative amount, zero amount) ✅
- Non-numeric accountId rejected ✅

**Out-of-Order**
- Events with different timestamps processed correctly ✅
- List returns sorted by DESC timestamp ✅

**Balance/State**
- Multiple credits/debits tracked ✅
- Amounts preserved correctly ✅
- Metadata stored correctly ✅
- Different transaction types handled ✅

### ✅ Trace Propagation Tests (12 tests)

**Trace ID Generation**
- UUID generated if not provided ✅
- Custom X-Trace-Id used if provided ✅
- W3C traceparent header parsed correctly ✅
- Blank/whitespace trace IDs regenerated ✅

**Trace ID Propagation**
- Trace ID in POST /events response ✅
- Trace ID in GET /events/{id} response ✅
- Trace ID in GET /events response ✅
- MDC set during request processing ✅
- Multiple requests maintain isolation ✅

### ✅ Integration Tests (9 tests)

**Complete Flows**
- POST → GET → LIST with trace propagation ✅
- Multiple events ordering verification ✅
- Idempotency at integration level ✅
- Validation errors proper handling ✅
- Trace IDs through all endpoints ✅
- Pagination across requests ✅
- Account isolation maintained ✅
- Large decimals handled ✅
- Multiple currencies supported ✅

### ✅ Transaction & Client Tests (10 tests)

**Transaction Data**
- Amount stored correctly ✅
- Type (CREDIT/DEBIT) stored ✅
- Currency stored correctly ✅
- Description preserved ✅
- Negative amounts for DEBIT ✅

**Client Trace Handling**
- Trace ID format valid ✅
- Trace IDs unique ✅
- Multiple transactions different amounts ✅
- Data structure preservation ✅
- Trace ID pass-through ✅

---

## API Endpoints Tested

### POST /events
- ✅ Valid event submission (201 CREATED)
- ✅ Duplicate event (409 CONFLICT)
- ✅ Missing fields (400 BAD REQUEST)
- ✅ Invalid types (400 BAD REQUEST)
- ✅ Invalid amounts (400 BAD REQUEST)
- ✅ Trace ID propagation

### GET /events/{eventId}
- ✅ Successful retrieval (200 OK)
- ✅ Not found (404 NOT FOUND)
- ✅ Trace ID propagation

### GET /events?account=X
- ✅ List with pagination (200 OK)
- ✅ Sorting by timestamp DESC
- ✅ Account isolation
- ✅ Missing account parameter (400 BAD REQUEST)
- ✅ Default pagination
- ✅ Trace ID propagation

---

## Trace ID Test Scenarios

```
Request                   Trace ID Handling
────────────────────────────────────────────
No header            →  Auto-generate UUID
X-Trace-Id: abc      →  Use "abc"
traceparent: xx-abc  →  Extract "abc"
Both headers         →  Use traceparent extracted

Response always includes X-Trace-Id header
MDC populated during request processing
MDC cleared after request completes
```

---

## Expected Test Results

```
========================================
  GATEWAY SERVICE TEST SUITE RESULTS
========================================

✅ EventControllerTest ................. 18 passed
✅ GatewayServiceCoreTest .............. 12 passed
✅ TracePropagationTest ................ 12 passed
✅ GatewayIntegrationTest .............. 9 passed
✅ AccountServiceClientTest ............ 10 passed

────────────────────────────────────────────
TOTAL: 61 tests, 61 passed, 0 failed
════════════════════════════════════════════

BUILD SUCCESS
```

---

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run tests
        run: mvn -f gateway-service/pom.xml test
      - name: Generate coverage
        run: mvn -f gateway-service/pom.xml jacoco:report
```

---

## Key Test Data

### Test Event
```json
{
  "eventId": "evt-001",
  "accountId": "123",
  "type": "CREDIT",
  "amount": 100.00,
  "currency": "USD",
  "eventTimestamp": "2026-07-11T10:30:00Z"
}
```

### Test Trace ID
```
X-Trace-Id: trace-12345-abcde
```

### W3C Traceparent Format
```
traceparent: 00-0af7651916cd43dd-b9c7c3d609704e0f-01
Extract trace ID: 0af7651916cd43dd
```

---

## Test Isolation & Setup

Each test includes:
- ✅ `@BeforeEach` setup with database cleanup
- ✅ Fresh test data (unique event IDs, account IDs)
- ✅ Automatic trace ID generation
- ✅ MockMvc or service layer setup

No test depends on another test's state.

---

## Common Test Patterns

### REST API Test
```java
mockMvc.perform(post("/events")
        .header(TraceFilter.TRACE_ID_HEADER, traceId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(toJson(eventRequest)))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.eventId").value(expectedId));
```

### Service Test
```java
EventResponse response = gatewayService.submitEvent(request, traceId);
assertEquals(expectedEventId, response.getEventId());
```

### Integration Test
```java
// POST
mockMvc.perform(post("/events")...)
    .andExpect(status().isCreated());

// GET
mockMvc.perform(get("/events/" + eventId)...)
    .andExpect(status().isOk());

// LIST
mockMvc.perform(get("/events")
        .param("account", accountId)...)
    .andExpect(status().isOk());
```

---

## Debugging Failed Tests

### Enable Debug Logging
```properties
# application-test.properties
logging.level.com.dj.gateway=DEBUG
logging.level.org.springframework=INFO
```

### View Full Request/Response
```java
mockMvc.perform(post("/events")...)
    .andDo(print())  // Print full HTTP exchange
    .andExpect(status().isCreated());
```

### Check Test Report
```bash
# HTML report
cat target/surefire-reports/index.html

# Console output
mvn test 2>&1 | grep -A 20 "FAILURES"
```

---

## Performance Notes

- All tests complete in < 5 seconds
- Database is in-memory (H2)
- No external service calls (mocked)
- Suitable for pre-commit hooks
- Can run in parallel if needed

---

## Next Steps

1. **Run all tests:** `mvn test`
2. **Fix any failures:** Check test logs
3. **Review coverage:** `mvn jacoco:report`
4. **Integrate in CI/CD:** Configure GitHub Actions
5. **Add more tests:** For new features

---

## Support

For issues:
1. Check TEST_DOCUMENTATION.md for detailed info
2. Look at test class comments for specific test details
3. Review test assertions for failure reasons
4. Add `@DisplayName` annotations for clarity
