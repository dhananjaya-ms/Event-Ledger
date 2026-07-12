# Event-Ledger: Microservices Event Processing System

A robust, production-ready microservices architecture for processing transaction events with strong consistency guarantees, idempotency enforcement, and comprehensive resilience patterns.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [Prerequisites](#prerequisites)
4. [Setup Instructions](#setup-instructions)
5. [Running the Services](#running-the-services)
6. [Running Tests](#running-tests)
7. [Resiliency Pattern](#resiliency-pattern)
8. [API Documentation](#api-documentation)
9. [Development](#development)
10. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

Event-Ledger is a distributed event processing system composed of two complementary microservices:

### **Gateway Service** (Public Entry Point)
- **Port**: 8081 (exposed as 8080 via Docker)
- **Role**: API gateway for client requests
- **Responsibilities**:
  - Receives transaction events (CREDIT/DEBIT) via REST API
  - Validates input according to strict business rules
  - Enforces **idempotency** using unique `eventId` constraints
  - Persists events to local H2 database (eventdb)
  - Forwards events to Account Service for processing
  - Provides event retrieval and history queries
  - Implements distributed tracing with trace ID propagation
  - Structured JSON logging for centralized log aggregation
  - Health checks and metrics collection
  - Swagger/OpenAPI documentation

### **Account Service** (Internal Worker)
- **Port**: 8080 (internal only, not exposed to clients)
- **Role**: Event processor and state manager
- **Responsibilities**:
  - Receives transaction events from Gateway Service
  - Manages account state (balances, transaction history)
  - Applies transactions to accounts
  - Stores account data in local H2 database (accountdb)
  - Provides account-level queries and reporting
  - Health checks and performance metrics

### **Communication Flow**

```
┌─────────────────┐
│   Client/User   │
└────────┬────────┘
         │ HTTP (POST /events)
         │
┌────────▼────────────────────┐
│   Gateway Service            │
│   - Validate & Persist      │
│   - Enforce Idempotency     │
│   - Apply Resilience4j      │
└────────┬────────────────────┘
         │ WebClient (HTTP)
         │ Circuit Breaker Protection
         │ Trace ID Propagation
         │
┌────────▼────────────────────┐
│   Account Service            │
│   - Process Transaction     │
│   - Update Balance          │
│   - Return Status           │
└─────────────────────────────┘
```

### **Key Architectural Principles**

1. **Idempotency by Design**: Every event has a unique `eventId` that acts as an idempotency key
2. **Exactly-Once Delivery**: Events forwarded to Account Service exactly once, with status tracking
3. **Async-Safe Concurrency**: Per-account locking prevents race conditions during concurrent processing
4. **Event Sourcing**: All events are immutable audit logs stored chronologically
5. **Resilience First**: Circuit breaker pattern protects against cascading failures
6. **Distributed Tracing**: Trace IDs flow through entire system for debugging and auditing

---

## Project Structure

```
Event-Ledger (Root)
├── docker-compose.yml                 # Docker orchestration for both services
├── docker-manager.ps1                 # PowerShell management script
├── README.md                          # This file
├── DOCKER_SETUP.md                    # Docker configuration guide
├── DOCKER_VALIDATION.md               # Docker validation checklist
│
├── Event-Ledger-Local/
│   └── account-service/               # Account Service (Worker)
│       ├── pom.xml                    # Maven dependencies
│       ├── Dockerfile                 # Docker image definition
│       ├── mvnw / mvnw.cmd           # Maven wrapper
│       └── src/
│           ├── main/java/com/dj/account/
│           │   ├── AccountServiceApplication.java
│           │   ├── controller/        # REST endpoints
│           │   ├── service/           # Business logic
│           │   ├── repository/        # Data access
│           │   ├── entity/            # JPA entities
│           │   ├── dto/               # Data transfer objects
│           │   └── exception/         # Custom exceptions
│           ├── main/resources/
│           │   ├── application.properties
│           │   └── logback-spring.xml
│           └── test/java/com/dj/account/
│               ├── AccountServiceApplicationTests.java
│               ├── controller/        # Controller tests
│               ├── service/           # Service tests
│               ├── repository/        # Repository tests
│               └── integration/       # Integration tests
│
└── Event-Ledger-Repo/
    └── gateway-service/               # Gateway Service (API Gateway)
        ├── pom.xml                    # Maven dependencies
        ├── Dockerfile                 # Docker image definition
        ├── mvnw / mvnw.cmd           # Maven wrapper
        ├── IMPLEMENTATION.md          # Implementation details
        ├── README_TESTS.md            # Test documentation
        ├── TEST_DOCUMENTATION.md      # Comprehensive test guide
        ├── TEST_QUICK_REFERENCE.md    # Quick test reference
        ├── TEST_IMPLEMENTATION_SUMMARY.md
        └── src/
            ├── main/java/com/dj/gateway/
            │   ├── GatewayServiceApplication.java
            │   ├── controller/        # REST endpoints
            │   │   ├── EventController.java
            │   │   ├── HealthController.java
            │   │   └── GlobalExceptionHandler.java
            │   ├── service/           # Business logic
            │   │   ├── GatewayService.java (interface)
            │   │   ├── GatewayServiceImpl.java
            │   │   └── AccountServiceClient.java
            │   ├── repository/        # Data access
            │   │   └── EventRepository.java
            │   ├── entity/            # JPA entities
            │   │   └── Event.java
            │   ├── dto/               # Data transfer objects
            │   │   ├── EventRequest.java
            │   │   ├── EventResponse.java
            │   │   └── TransactionRequest.java
            │   ├── exception/         # Custom exceptions
            │   │   ├── IdempotencyException.java
            │   │   └── EventNotFoundException.java
            │   ├── web/               # Web configuration
            │   │   └── TraceFilter.java
            │   ├── config/            # Spring configuration
            │   │   ├── WebClientConfig.java
            │   │   ├── GatewayRouteConfig.java
            │   │   └── OpenApiConfig.java
            │   └── util/              # Utility classes
            │       └── EventMapper.java
            ├── main/resources/
            │   ├── application.properties
            │   ├── application-docker.properties
            │   └── logback-spring.xml
            └── test/java/com/dj/gateway/
                ├── controller/        # Controller tests
                │   └── EventControllerTest.java
                ├── service/           # Service tests
                │   ├── GatewayServiceCoreTest.java
                │   └── AccountServiceClientTest.java
                ├── web/               # Web layer tests
                │   └── TracePropagationTest.java
                └── integration/       # Integration tests
                    └── GatewayIntegrationTest.java
```

---

## Prerequisites

### System Requirements
- **OS**: Windows, macOS, or Linux
- **RAM**: 4GB minimum (8GB recommended)
- **Disk Space**: 3GB for Docker images and containers

### Software Requirements

#### Option 1: Docker Compose (Recommended)
- **Docker Desktop**: 20.10+ ([download](https://www.docker.com/products/docker-desktop))
- **Docker Compose**: 2.0+ (included with Docker Desktop)

Verify installation:
```powershell
docker --version
docker-compose --version
```

#### Option 2: Manual (Local Development)
- **Java**: 17+ ([download OpenJDK 17](https://jdk.java.net/17/))
- **Maven**: 3.8.1+ ([download](https://maven.apache.org/download.cgi))
- **Git**: 2.30+ ([download](https://git-scm.com/))

Verify installation:
```powershell
java -version
mvn -version
git --version
```

---

## Setup Instructions

### 1. Clone the Repository

```powershell
# Navigate to your workspace
cd C:\Users\Dhananjaya Bal\git

# The directories should already exist:
# - Event-Ledger\
# - Event-Ledger-Local\account-service\
# - Event-Ledger-Repo\gateway-service\
```

### 2. Install Dependencies (Manual Setup Only)

If running without Docker, download and compile dependencies:

**Gateway Service:**
```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Repo\gateway-service
.\mvnw.cmd clean install
```

**Account Service:**
```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Local\account-service
.\mvnw.cmd clean install
```

### 3. Docker Setup (Recommended)

Navigate to the root directory and verify the compose file:

```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger
cat docker-compose.yml
```

---

## Running the Services

### **Option A: Docker Compose (Recommended)**

#### Quick Start

```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger

# Build and start both services
docker-compose up -d

# Verify services are running
docker-compose ps
```

Expected output:
```
CONTAINER ID   IMAGE              STATUS           PORTS
xxx            account-service    Up (healthy)     8080/tcp
yyy            gateway-service    Up (healthy)     0.0.0.0:8080->8081/tcp
```

#### Monitor Logs

```powershell
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f gateway-service
docker-compose logs -f account-service

# View logs with timestamps
docker-compose logs --timestamps -f
```

#### Stop Services

```powershell
# Stop containers (data persists in memory for this session)
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop, remove containers, and clean volumes
docker-compose down -v
```

#### Rebuild After Code Changes

```powershell
# Rebuild images and restart services
docker-compose up -d --build
```

### **Option B: Manual Startup (Local Development)**

Run each service in a separate terminal.

**Terminal 1: Account Service**
```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Local\account-service
.\mvnw.cmd spring-boot:run
```

Expected output:
```
2026-07-12 10:30:00.123  INFO 1234 --- [ main] com.dj.account.AccountServiceApplication:
Started AccountServiceApplication in 5.123 seconds
```

**Terminal 2: Gateway Service**
```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Repo\gateway-service
.\mvnw.cmd spring-boot:run
```

Expected output:
```
2026-07-12 10:30:10.456  INFO 5678 --- [ main] com.dj.gateway.GatewayServiceApplication:
Started GatewayServiceApplication in 6.789 seconds
```

---

## Accessing the Services

Once both services are running, access them via:

### Gateway Service (Public)

| Resource | URL |
|----------|-----|
| **Base API** | `http://localhost:8080` |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |
| **OpenAPI Spec** | `http://localhost:8080/v3/api-docs` |
| **Health Check** | `http://localhost:8080/actuator/health` |
| **Metrics** | `http://localhost:8080/actuator/metrics` |
| **Circuit Breaker Status** | `http://localhost:8080/actuator/health/circuitbreakers` |

### Account Service (Internal)

| Resource | URL |
|----------|-----|
| **Base API** | `http://localhost:8080` (manual) or `http://account-service:8080` (Docker) |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |
| **H2 Console** | `http://localhost:8080/h2-console` |
| **Health Check** | `http://localhost:8080/actuator/health` |
| **Metrics** | `http://localhost:8080/actuator/metrics` |

> **Note**: Account Service is only accessible locally or within Docker network. It's not exposed to clients directly.

---

## API Examples

### Create a Transaction Event

```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: trace-001" \
  -d '{
    "eventId": "evt-12345",
    "accountId": "acct-001",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-07-12T10:30:00Z",
    "metadata": {
      "source": "external-api",
      "batchId": "batch-001"
    }
  }'
```

**Expected Response (201 Created):**
```json
{
  "eventId": "evt-12345",
  "accountId": "acct-001",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-07-12T10:30:00Z",
  "status": "APPLIED",
  "createdAt": "2026-07-12T10:30:00.123Z",
  "processedAt": "2026-07-12T10:30:00.456Z"
}
```

### Retrieve a Single Event

```bash
curl -X GET http://localhost:8080/events/evt-12345 \
  -H "X-Trace-Id: trace-001"
```

### List Events for an Account

```bash
curl -X GET "http://localhost:8080/events?account=acct-001&page=0&size=20" \
  -H "X-Trace-Id: trace-001"
```

---

## Running Tests

### Quick Start

```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Repo\gateway-service
.\mvnw.cmd test
```

### Run Account Service Tests

```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Local\account-service
.\mvnw.cmd test
```

### Run Specific Test Class

```powershell
cd gateway-service

# Controller layer tests
.\mvnw.cmd test -Dtest=EventControllerTest

# Service layer tests
.\mvnw.cmd test -Dtest=GatewayServiceCoreTest

# Trace propagation tests
.\mvnw.cmd test -Dtest=TracePropagationTest

# Integration tests
.\mvnw.cmd test -Dtest=GatewayIntegrationTest

# Client tests
.\mvnw.cmd test -Dtest=AccountServiceClientTest
```

### Run Single Test Method

```powershell
.\mvnw.cmd test -Dtest=EventControllerTest#testPostEvent_Success
.\mvnw.cmd test -Dtest=TracePropagationTest#testTrace_PropagatedInPostEventResponse
```

### View Test Reports

Test results are generated in `target/surefire-reports/`:

```powershell
cd gateway-service
.\mvnw.cmd test
# Open target/surefire-reports/index.html in browser
```

### Test Coverage Summary

**Gateway Service Tests**: 61 total tests
- ✅ 18 Controller layer tests (REST API endpoints)
- ✅ 12 Service layer tests (business logic)
- ✅ 12 Trace propagation tests (distributed tracing)
- ✅ 10 Client tests (Account Service integration)
- ✅ 9 Integration tests (end-to-end workflows)

**Account Service Tests**: Comprehensive coverage
- ✅ Controller tests
- ✅ Service tests
- ✅ Repository tests
- ✅ Integration tests

### Test Documentation

For comprehensive test information, see:
- `gateway-service/README_TESTS.md` - Executive summary
- `gateway-service/TEST_DOCUMENTATION.md` - Detailed test guide
- `gateway-service/TEST_QUICK_REFERENCE.md` - Quick command reference
- `gateway-service/TEST_IMPLEMENTATION_SUMMARY.md` - Architecture and metrics

---

## Resiliency Pattern

Event-Ledger implements a **Circuit Breaker** pattern using **Resilience4j** to protect against cascading failures when the Account Service becomes unavailable.

### **What is a Circuit Breaker?**

A circuit breaker is a design pattern that prevents cascading failures by:
1. **Monitoring** calls to a remote service
2. **Detecting** failures (timeouts, exceptions, slow responses)
3. **Opening** the circuit (failing fast) when threshold is exceeded
4. **Recovering** gracefully without overwhelming the failing service

### **Circuit Breaker States**

```
CLOSED (Normal)
    │
    ├─→ Requests flow normally
    ├─→ Failures are monitored
    ├─→ If failure rate exceeds threshold → OPEN
    │
OPEN (Failing)
    │
    ├─→ Requests fail immediately (no timeout wait)
    ├─→ Protects downstream service from overload
    ├─→ After wait duration → HALF_OPEN
    │
HALF_OPEN (Testing Recovery)
    │
    ├─→ Limited requests allowed to test recovery
    ├─→ If requests succeed → CLOSED
    ├─→ If requests fail → OPEN
```

### **Configuration in Event-Ledger**

The gateway-service protects calls to Account Service with these settings:

| Parameter | Value | Purpose |
|-----------|-------|---------|
| **Failure Rate Threshold** | 50% | Open circuit if 50%+ requests fail |
| **Slow Call Rate Threshold** | 50% | Count >5000ms calls as failures |
| **Slow Call Duration** | 5000ms | Threshold for slow calls |
| **Min Calls Required** | 5 | Evaluate rate only after 5 calls |
| **Window Size** | 10 | Track last 10 calls |
| **Permitted Calls (Half-Open)** | 3 | Allow 3 test calls during recovery |
| **Wait Duration** | 30000ms | Wait 30 seconds before retry |

### **Real-World Example**

**Scenario**: Account Service suddenly crashes

**Timeline:**
1. **T0**: Account Service crashes
2. **T0-T10s**: Gateway detects failures (50%+ failure rate)
3. **T10s**: Circuit opens, Gateway immediately rejects new requests
4. **T10s-T40s**: Gateway fails fast without waiting, allowing Account Service to recover
5. **T40s**: Circuit enters HALF_OPEN state, allows 3 test requests
6. **T40s+**: If Account Service recovers, circuit closes and traffic resumes

**Benefits:**
- ✅ **Prevents cascading failures**: Doesn't overwhelm failing service
- ✅ **Fast failure**: Clients get errors immediately instead of timeouts
- ✅ **Graceful degradation**: Service remains responsive under stress
- ✅ **Automatic recovery**: Periodically tests downstream service

### **Monitoring Circuit Breaker Health**

Check circuit breaker status via actuator endpoint:

```bash
# Get circuit breaker state
curl http://localhost:8080/actuator/health/circuitbreakers

# Response example:
{
  "components": {
    "accountService": {
      "status": "UP",
      "details": {
        "state": "CLOSED",
        "failureRate": "0.0%",
        "slowCallRate": "0.0%"
      }
    }
  }
}
```

### **View Metrics**

```bash
# Get all metrics
curl http://localhost:8080/actuator/metrics

# Circuit breaker specific metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

### **Why This Pattern for Event-Ledger?**

1. **Temporal Decoupling**: Services don't need to be available simultaneously
2. **Graceful Degradation**: System continues operating even when Account Service is down
3. **Resource Protection**: Prevents thread exhaustion from waiting on failed calls
4. **Automatic Recovery**: Periodically retries without manual intervention
5. **Production-Ready**: Battle-tested pattern used by Netflix, AWS, and major platforms

---

## Development

### Building the Projects

**Gateway Service:**
```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Repo\gateway-service
.\mvnw.cmd clean package
```

**Account Service:**
```powershell
cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Local\account-service
.\mvnw.cmd clean package
```

### IDE Setup

#### IntelliJ IDEA
1. Open project root directory
2. Maven should auto-detect pom.xml files
3. Configure SDK: Java 17
4. Run → Edit Configurations → Add Maven configurations for:
   - Account Service: `spring-boot:run` in account-service directory
   - Gateway Service: `spring-boot:run` in gateway-service directory

#### Eclipse
1. File → Import → Maven → Existing Maven Projects
2. Select root directory
3. Accept both projects (account-service and gateway-service)
4. Configure Java 17 as project JDK
5. Run → Run Configurations → Maven Build

#### Visual Studio Code
1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Maven for Java
2. Open workspace root
3. VSCode auto-detects Maven projects
4. Use Command Palette → Spring Boot Dashboard to run services

### Code Structure

**Gateway Service** (`com.dj.gateway.*`):
- **Controllers**: REST endpoints (`EventController`, `HealthController`)
- **Services**: Business logic with resilience4j (`GatewayService`, `AccountServiceClient`)
- **Entities**: JPA models (`Event`)
- **DTOs**: Request/Response objects
- **Exceptions**: Domain-specific exceptions
- **Web**: Filters and configuration (`TraceFilter`)

**Account Service** (`com.dj.account.*`):
- **Controllers**: REST endpoints
- **Services**: Transaction processing
- **Entities**: JPA models for accounts
- **DTOs**: Request/Response objects
- **Repositories**: Database queries
- **Metrics**: Prometheus metrics

### Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot | 3.5.16 | Microservice framework |
| Spring Data JPA | 3.5.16 | ORM and database access |
| H2 Database | Latest | In-memory test database |
| Resilience4j | Latest | Circuit breaker pattern |
| Springdoc OpenAPI | 2.1.0+ | Swagger/OpenAPI documentation |
| Micrometer Prometheus | Latest | Metrics collection |
| Logstash Logback | 7.4 | Structured JSON logging |

---

## Troubleshooting

### Docker Issues

#### Port 8080 Already in Use

```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process by PID
taskkill /PID <PID> /F

# Or change port in docker-compose.yml:
# ports:
#   - "8081:8081"
```

#### Containers Won't Start

```powershell
# Check logs for errors
docker-compose logs

# Rebuild images
docker-compose down -v
docker-compose up -d --build

# Check health status
docker-compose ps
```

#### Network Errors Between Services

```powershell
# Verify network exists
docker network ls

# Inspect network
docker network inspect event-ledger-network

# Check if containers can communicate
docker-compose exec gateway-service ping account-service
```

### Java/Maven Issues

#### Maven Build Fails

```powershell
# Clear local repository cache
rm -r $env:USERPROFILE\.m2\repository

# Rebuild with clean
.\mvnw.cmd clean package -U
```

#### Java Version Mismatch

```powershell
# Verify Java version
java -version

# Should show version 17.x.x

# Download Java 17
# https://jdk.java.net/17/
```

#### Tests Fail Locally

```powershell
# Run with debug output
.\mvnw.cmd test -X

# Run single test for isolation
.\mvnw.cmd test -Dtest=EventControllerTest#testPostEvent_Success

# Check for port conflicts
# Ensure account-service is running on 8080
# Ensure gateway-service is running on 8081
```

### Application Issues

#### Account Service Unreachable from Gateway

```powershell
# Check application.properties
cat gateway-service/src/main/resources/application.properties
# Should have: account-service.url=http://localhost:8080 (manual)
# Or in Docker: http://account-service:8080

# Check logs for connection errors
docker-compose logs gateway-service | findstr -i "account"

# Test connectivity
docker-compose exec gateway-service curl http://account-service:8080/actuator/health
```

#### Idempotency Not Working

**Symptom**: Duplicate events create new records instead of returning 409

```powershell
# Check if eventId column has UNIQUE constraint
docker exec -it gateway-service sh
sqlite3 eventdb ".schema event"

# Check logs for duplicate key errors
docker-compose logs gateway-service | findstr -i "unique\|duplicate"
```

#### Trace IDs Not Propagating

**Symptom**: No X-Trace-Id in response headers

```powershell
# Check TraceFilter is registered
docker-compose logs gateway-service | findstr -i "TraceFilter"

# Test trace propagation manually
curl -v -X GET http://localhost:8080/events/evt-123 \
  -H "X-Trace-Id: test-trace-001"

# Check response headers for X-Trace-Id
```

### Performance Issues

#### Slow Responses

```powershell
# Check metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# Monitor CPU and memory
docker stats

# Check logs for slow database queries
docker-compose logs gateway-service | findstr -i "slow\|duration"
```

#### High Memory Usage

```powershell
# In-memory H2 databases can grow
# Restart containers to clear
docker-compose down -v
docker-compose up -d

# Check heap size
docker stats --no-stream
```

---

## Additional Resources

### Documentation
- **DOCKER_SETUP.md**: Comprehensive Docker configuration guide
- **DOCKER_VALIDATION.md**: Docker validation checklist
- **gateway-service/IMPLEMENTATION.md**: Gateway Service implementation details
- **gateway-service/README_TESTS.md**: Test suite overview
- **gateway-service/TEST_DOCUMENTATION.md**: Comprehensive test guide

### External Resources
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [OpenAPI/Swagger](https://swagger.io/)
- [H2 Database Documentation](https://www.h2database.com/)

### Getting Help

1. **Check logs**: `docker-compose logs -f [service-name]`
2. **Review test documentation**: `gateway-service/TEST_DOCUMENTATION.md`
3. **Check API Swagger**: `http://localhost:8080/swagger-ui.html`
4. **Inspect metrics**: `http://localhost:8080/actuator/metrics`
5. **Verify health**: `http://localhost:8080/actuator/health`

---

## Quick Command Reference

```powershell
# Docker operations
docker-compose up -d                    # Start all services
docker-compose down                     # Stop all services
docker-compose logs -f                  # View logs
docker-compose ps                       # Check status
docker-compose up -d --build            # Rebuild and start

# Local development
cd gateway-service && .\mvnw.cmd spring-boot:run      # Start gateway
cd account-service && .\mvnw.cmd spring-boot:run      # Start account

# Testing
.\mvnw.cmd test                         # Run all tests
.\mvnw.cmd test -Dtest=<ClassName>     # Run specific test class
.\mvnw.cmd test -Dtest=<Class>#<Method> # Run specific test method

# Building
.\mvnw.cmd clean package               # Full build
.\mvnw.cmd clean install               # Build with dependencies

# API Testing
curl http://localhost:8080/swagger-ui.html  # Access Swagger UI
curl http://localhost:8080/actuator/health  # Health check
curl http://localhost:8080/actuator/metrics # Metrics
```

---

## License

This project is provided as-is for educational and commercial use.

---

## Version Information

| Component | Version | Java | Spring Boot | Status |
|-----------|---------|------|-------------|--------|
| Gateway Service | 0.0.1-SNAPSHOT | 17+ | 3.5.16 | ✅ Production Ready |
| Account Service | 0.0.1-SNAPSHOT | 17+ | 3.5.16 | ✅ Production Ready |

---

## Summary

Event-Ledger provides a robust foundation for processing transaction events with:

✅ **Strong Consistency**: Idempotency enforcement and exactly-once delivery
✅ **High Availability**: Circuit breaker pattern prevents cascading failures
✅ **Observability**: Distributed tracing and structured JSON logging
✅ **Production-Ready**: Comprehensive tests and Docker support
✅ **Developer-Friendly**: Clear APIs, Swagger documentation, and examples

Start with Docker Compose for quickest onboarding, or run locally for development work.

**Get Started**: `docker-compose up -d` → `http://localhost:8080/swagger-ui.html`
