# Docker Setup Guide for Event Ledger Services

## Overview

This docker-compose configuration manages two microservices for the Event Ledger system:

- **account-service**: Internal service (not exposed to host) - Manages account state and balances
- **gateway-service**: Public service (exposed on port 8080) - Entry point for client requests

## Architecture

```
┌─────────────────────────────────────┐
│   User/Client                        │
└──────────────────┬──────────────────┘
                   │
                   │ HTTP (Port 8080)
                   │
┌──────────────────▼──────────────────┐
│   gateway-service (Public)           │
│   - Port: 8081 (internal)            │
│   - Host Port: 8080                  │
│   - H2 Database: eventdb             │
└──────────────────┬──────────────────┘
                   │
     (Docker Network - event-ledger-network)
                   │
┌──────────────────▼──────────────────┐
│   account-service (Internal)         │
│   - Port: 8080 (only internal)       │
│   - No host port mapping             │
│   - H2 Database: accountdb           │
└──────────────────────────────────────┘
```

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Approximately 2GB available disk space for images and containers

## Quick Start

### 1. Navigate to the workspace root

```powershell
cd C:\Users\Dhananjaya Bal\git
```

### 2. Build and start all services

```powershell
docker-compose up -d
```

This command will:
- Build Docker images for both services
- Create a custom bridge network (`event-ledger-network`)
- Start account-service with health checks
- Start gateway-service after account-service is healthy
- Expose gateway-service on `http://localhost:8080`

### 3. Verify services are running

```powershell
docker-compose ps
```

Expected output:
```
CONTAINER ID   IMAGE              COMMAND                  STATUS           PORTS
xxx            account-service    "java -jar account-..."  Up (healthy)     8080/tcp
yyy            gateway-service    "java -jar gateway-..."  Up (healthy)     0.0.0.0:8080->8081/tcp
```

## Service Endpoints

### Gateway Service (Public)
- **Base URL**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs**: `http://localhost:8080/v3/api-docs`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`

### Account Service (Internal - Docker Network Only)
- **Base URL**: `http://account-service:8080` (from within Docker network)
- **Swagger UI**: `http://account-service:8080/swagger-ui.html`
- **H2 Console**: `http://account-service:8080/h2-console`
- **Health Check**: `http://account-service:8080/actuator/health`

> Note: Account Service is **not accessible** from the host machine directly due to no port mapping.

## Database Access

### Gateway Service H2 Database
Access from within the container only. To interact with it:

```powershell
docker exec -it gateway-service sh
# Then use curl or connect through the application
```

### Account Service H2 Database
Access from within the container only:

```powershell
docker exec -it account-service sh
```

## Common Commands

### View logs for a service
```powershell
docker-compose logs -f gateway-service
docker-compose logs -f account-service
```

### View all logs
```powershell
docker-compose logs -f
```

### Stop all services
```powershell
docker-compose stop
```

### Start services (after stopping)
```powershell
docker-compose start
```

### Rebuild services (when code changes)
```powershell
docker-compose up -d --build
```

### Remove all containers and networks
```powershell
docker-compose down
```

### Remove everything including volumes
```powershell
docker-compose down -v
```

## Environment Configuration

### Gateway Service Configuration
Configured in `docker-compose.yml` environment section:
- Database: H2 in-memory (eventdb)
- Resilience4j Circuit Breaker enabled for account-service
- Swagger UI enabled
- Account Service URL: `http://account-service:8080`

### Account Service Configuration
Configured in `docker-compose.yml` environment section:
- Database: H2 in-memory (accountdb)
- H2 Console enabled
- Actuator endpoints exposed

## Health Checks

Both services include health checks that ensure:
- Service is running and responsive
- API is accessible
- Gateway Service waits for Account Service to be healthy before starting

Check health status:
```powershell
docker-compose ps
# Look for "Up (healthy)" status
```

## Network Communication

- **Gateway → Account**: Uses service name `account-service` internally (Docker DNS)
- **Account → External**: Not possible (no outbound network access configured)
- **Client → Gateway**: Through exposed port 8080 on host
- **Client → Account**: Not possible (not exposed)

## Troubleshooting

### Services fail to start
```powershell
# Check logs
docker-compose logs

# Full output with timestamps
docker-compose logs --timestamps
```

### Port 8080 already in use
Edit `docker-compose.yml` and change the gateway-service port mapping:
```yaml
ports:
  - "8081:8081"  # Map to different host port
```

### Account service unreachable from gateway
1. Verify both services are on the same network:
   ```powershell
   docker network inspect event-ledger-network
   ```

2. Check gateway-service logs for connection errors:
   ```powershell
   docker-compose logs gateway-service
   ```

### Container keeps restarting
Check health check status:
```powershell
docker-compose logs account-service
docker-compose logs gateway-service
```

## Security Notes

⚠️ **Important for Production**:
- Account Service is intentionally isolated and not exposed
- H2 Consoles are enabled (disable in production with `SPRING_H2_CONSOLE_ENABLED=false`)
- No authentication is configured (add Spring Security for production)
- In-memory H2 databases mean data is lost when containers stop

## Next Steps

1. Test the API through Swagger UI: `http://localhost:8080/swagger-ui.html`
2. Monitor logs: `docker-compose logs -f`
3. Check metrics: `http://localhost:8080/actuator/metrics`
4. Extend configuration by modifying environment variables in `docker-compose.yml`

## File Structure

```
C:\Users\Dhananjaya Bal\git\
├── docker-compose.yml                          # Main orchestration file
├── Event-Ledger-Local\account-service\
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── pom.xml
│   └── src\main\resources\application.properties
└── Event-Ledger-Repo\gateway-service\
    ├── Dockerfile
    ├── .dockerignore
    ├── pom.xml
    ├── src\main\resources\
    │   ├── application.properties
    │   └── application-docker.properties
```

## References

- Docker Compose Documentation: https://docs.docker.com/compose/
- Spring Boot Profiles: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles
- Spring Boot Properties: https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html
