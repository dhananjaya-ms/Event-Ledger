# Metrics Quick Reference

## Test the Implementation

### 1. Start the Application
```bash
mvn spring-boot:run
```

### 2. Make Test Requests
```bash
# Create a transaction (should succeed)
curl -X POST http://localhost:8080/accounts/123/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":"DEBIT","amount":100}'

# Get account balance
curl http://localhost:8080/accounts/123/balance

# Get account details
curl http://localhost:8080/accounts/123

# Trigger an error
curl http://localhost:8080/accounts/invalid/balance
```

### 3. View Metrics via REST API

#### Get Overall Summary
```bash
curl http://localhost:8080/metrics/summary
```

#### Get All Tracked Endpoints
```bash
curl http://localhost:8080/metrics/endpoints
```

#### Get Metrics for Specific Endpoint
```bash
curl "http://localhost:8080/metrics/endpoint?name=GET%20/accounts/{id}"
```

### 4. View Actuator Metrics
```bash
# All available metrics
curl http://localhost:8080/actuator/metrics

# Specific metric details
curl http://localhost:8080/actuator/metrics/http.request.count
curl http://localhost:8080/actuator/metrics/http.request.duration

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

### 5. Check Logs
```bash
# Look for lines like:
# Request Metrics - Endpoint: GET /accounts/{id}, Total Requests: X, Errors: Y, Error Rate: Z%, Latency: Nms
```

## Metrics Data Points

Each endpoint tracks:
- ✓ Total Request Count
- ✓ Error Count
- ✓ Error Rate (%)
- ✓ Min Latency (ms)
- ✓ Max Latency (ms)
- ✓ Average Latency (ms)

## Response Format Example

```json
{
  "totalRequests": 10,
  "totalErrors": 2,
  "overallErrorRate": "20.00%",
  "endpointMetrics": {
    "GET /accounts/{id}": {
      "endpoint": "GET /accounts/{id}",
      "totalRequests": 5,
      "errorCount": 1,
      "errorRate": "20.00%",
      "avgLatency": "45.20ms",
      "minLatency": 32,
      "maxLatency": 78
    }
  }
}
```

## Integration Points

- **Micrometer** - Records metrics in standard format
- **Spring Boot Actuator** - Exposes metrics via `/actuator/metrics`
- **Prometheus** - Exports metrics via `/actuator/prometheus`
- **Custom REST API** - `/metrics/*` endpoints
- **Application Logs** - INFO level logs from com.dj.account.metrics

## Key Features

✅ **Automatic Tracking** - All HTTP requests are tracked automatically
✅ **Endpoint Normalization** - Similar endpoints grouped together (e.g., /accounts/{id})
✅ **Error Detection** - HTTP status >= 400 marked as errors
✅ **Latency Measurement** - Request duration tracked in milliseconds
✅ **Multiple Exposure** - REST API, Actuator, Prometheus, and Logs
✅ **Thread-Safe** - Uses ConcurrentHashMap for concurrent access
