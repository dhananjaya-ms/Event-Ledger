# Custom Metrics Implementation - Account Service

## Overview

This implementation adds comprehensive request metrics tracking to the account service with the following capabilities:

1. **Request Count by Endpoint** - Tracks total requests per endpoint
2. **Error Rate** - Calculates error percentage per endpoint
3. **Latency Histogram** - Records min, max, and average latency per endpoint

## Architecture

### Components Created

#### 1. **RequestMetricsService** (`metrics/RequestMetricsService.java`)
- Central service for metrics collection and management
- Uses Micrometer to record metrics
- Maintains in-memory metrics storage per endpoint
- Provides methods to retrieve metrics

**Key Features:**
- `recordSuccess()` - Record successful requests
- `recordError()` - Record failed requests
- `getAllMetrics()` - Get metrics for all endpoints
- `getMetrics(endpoint)` - Get metrics for specific endpoint

#### 2. **MetricsInterceptor** (`metrics/MetricsInterceptor.java`)
- Spring Web Interceptor that intercepts all HTTP requests
- Records request start time
- Calculates duration in afterCompletion
- Normalizes endpoint paths (replaces IDs with `{id}`)
- Determines if request was successful or error based on HTTP status

#### 3. **MetricsConfig** (`config/MetricsConfig.java`)
- WebMvcConfigurer configuration class
- Registers MetricsInterceptor globally

#### 4. **MetricsController** (`controller/MetricsController.java`)
- REST API to expose metrics
- **Endpoints:**
  - `GET /metrics/summary` - Overall metrics across all endpoints
  - `GET /metrics/endpoint?name=<endpoint>` - Metrics for specific endpoint
  - `GET /metrics/endpoints` - List all tracked endpoints

### Metrics Exposure Methods

#### Method 1: REST Endpoint
Access metrics via the custom REST API:
```
GET http://localhost:8080/metrics/summary
GET http://localhost:8080/metrics/endpoints
GET http://localhost:8080/metrics/endpoint?name=GET%20/accounts/{id}
```

#### Method 2: Spring Boot Actuator
Access Micrometer metrics via actuator endpoints:
```
GET http://localhost:8080/actuator/metrics
GET http://localhost:8080/actuator/metrics/http.request.count
GET http://localhost:8080/actuator/metrics/http.request.duration
```

#### Method 3: Prometheus
Export metrics in Prometheus format:
```
GET http://localhost:8080/actuator/prometheus
```

#### Method 4: Logs
Metrics are automatically logged at INFO level for each request:
```
Request Metrics - Endpoint: GET /accounts/{id}, Total Requests: 5, Errors: 1, Error Rate: 20.00%, Latency: 45ms
```

## Configuration

### application.properties
```properties
# Actuator - Enable metrics endpoints
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true

# Log metrics in JSON format
logging.level.com.dj.account.metrics=INFO
```

### Dependencies Added to pom.xml
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

## Usage Examples

### Example 1: Get Overall Metrics Summary
```bash
curl http://localhost:8080/metrics/summary
```

Response:
```json
{
  "totalRequests": 15,
  "totalErrors": 2,
  "overallErrorRate": "13.33%",
  "endpointMetrics": {
    "GET /accounts/{id}": {
      "endpoint": "GET /accounts/{id}",
      "totalRequests": 5,
      "errorCount": 1,
      "errorRate": "20.00%",
      "avgLatency": "45.20ms",
      "minLatency": 32,
      "maxLatency": 78
    },
    "POST /accounts/{id}/transactions": {
      "endpoint": "POST /accounts/{id}/transactions",
      "totalRequests": 10,
      "errorCount": 1,
      "errorRate": "10.00%",
      "avgLatency": "52.10ms",
      "minLatency": 28,
      "maxLatency": 95
    }
  }
}
```

### Example 2: Get Metrics for Specific Endpoint
```bash
curl "http://localhost:8080/metrics/endpoint?name=GET%20/accounts/{id}"
```

Response:
```json
{
  "endpoint": "GET /accounts/{id}",
  "totalRequests": 5,
  "errorCount": 1,
  "errorRate": "20.00%",
  "avgLatency": "45.20ms",
  "minLatency": 32,
  "maxLatency": 78
}
```

### Example 3: View Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus | grep http_request
```

Output:
```
# HELP http_request_count_total  
# TYPE http_request_count_total counter
http_request_count_total{endpoint="GET /accounts/{id}",status="success"} 4.0
http_request_count_total{endpoint="GET /accounts/{id}",status="error"} 1.0

# HELP http_request_duration_seconds  
# TYPE http_request_duration_seconds histogram
http_request_duration_seconds{endpoint="GET /accounts/{id}",status="success",quantile="0.5"} 0.045
http_request_duration_seconds{endpoint="GET /accounts/{id}",status="success",quantile="0.95"} 0.078
http_request_duration_seconds{endpoint="GET /accounts/{id}",status="success",quantile="0.99"} 0.095
```

### Example 4: View Logs
Each request generates a log entry:
```
Request Metrics - Endpoint: GET /accounts/{id}, Total Requests: 5, Errors: 1, Error Rate: 20.00%, Latency: 32ms
Request Metrics - Endpoint: POST /accounts/{id}/transactions, Total Requests: 10, Errors: 1, Error Rate: 10.00%, Latency: 95ms
```

## Metrics Tracked

### Per-Endpoint Metrics
- **Total Requests**: Count of all requests to an endpoint
- **Error Count**: Count of failed requests (status >= 400)
- **Error Rate**: Percentage of failed requests
- **Min Latency**: Minimum request duration in milliseconds
- **Max Latency**: Maximum request duration in milliseconds
- **Average Latency**: Mean request duration in milliseconds

### Micrometer Metrics
- `http.request.count` - Counter with tags: endpoint, status
- `http.request.duration` - Timer with percentiles: 0.5, 0.95, 0.99

## How It Works

1. **Request Arrives** → MetricsInterceptor.preHandle() records start time
2. **Request Processing** → Normal Spring MVC processing
3. **Response Sent** → MetricsInterceptor.afterCompletion() records metrics
4. **Metrics Storage** → Stored in RequestMetricsService with Micrometer
5. **Metrics Exposed** → Available via REST API, Actuator, Prometheus, and Logs

## Endpoint Path Normalization

Endpoint paths are normalized to group similar requests:
- `/accounts/123/balance` → `/accounts/{id}/balance`
- `/accounts/456/balance` → `/accounts/{id}/balance`

This ensures that requests to different resources of the same type are grouped together.

## Future Enhancements

1. Add custom metrics for business events
2. Integrate with external metrics systems (Grafana, Splunk)
3. Add alerting based on error rate thresholds
4. Implement request tracing correlation IDs
5. Add custom histogram buckets for latency percentiles
6. Implement metrics export to time-series databases

## Troubleshooting

### Metrics Not Showing
1. Verify actuator endpoints are enabled in application.properties
2. Check that log level for com.dj.account.metrics is set to INFO or DEBUG
3. Ensure MetricsConfig is registered (should be auto-scanned)

### High Memory Usage
The in-memory metrics storage could grow if too many unique endpoint paths are created. Consider implementing:
- Periodic cleanup of old metrics
- Limiting endpoint path combinations
- Exporting to external storage

## Files Modified/Created

### Created Files:
- `src/main/java/com/dj/account/metrics/RequestMetricsService.java`
- `src/main/java/com/dj/account/metrics/MetricsInterceptor.java`
- `src/main/java/com/dj/account/config/MetricsConfig.java`
- `src/main/java/com/dj/account/controller/MetricsController.java`

### Modified Files:
- `pom.xml` - Added micrometer-registry-prometheus dependency
- `src/main/resources/application.properties` - Added actuator and metrics configuration
