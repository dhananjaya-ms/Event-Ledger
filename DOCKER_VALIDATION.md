# Docker Setup Validation Guide

This guide helps you verify that the Docker setup is correctly configured and working.

## ✅ Pre-Deployment Checklist

### 1. Environment Verification

- [ ] **Docker Installed**
  ```powershell
  docker --version
  # Expected: Docker version 20.10+
  ```

- [ ] **Docker Compose Installed**
  ```powershell
  docker-compose --version
  # Expected: Docker Compose version 2.0+
  ```

- [ ] **Docker Daemon Running**
  ```powershell
  docker ps
  # Should show list of containers (may be empty)
  ```

- [ ] **Working Directory Correct**
  ```powershell
  pwd
  # Should be: C:\Users\Dhananjaya Bal\git
  ```

### 2. File Structure Verification

- [ ] **docker-compose.yml exists**
  ```
  C:\Users\Dhananjaya Bal\git\docker-compose.yml
  ```

- [ ] **Account Service Dockerfile exists**
  ```
  C:\Users\Dhananjaya Bal\git\Event-Ledger-Local\account-service\Dockerfile
  ```

- [ ] **Gateway Service Dockerfile exists**
  ```
  C:\Users\Dhananjaya Bal\git\Event-Ledger-Repo\gateway-service\Dockerfile
  ```

- [ ] **application-docker.properties exists**
  ```
  C:\Users\Dhananjaya Bal\git\Event-Ledger-Repo\gateway-service\src\main\resources\application-docker.properties
  ```

### 3. Maven Build Verification

- [ ] **Account Service Builds Successfully**
  ```powershell
  cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Local\account-service
  mvn clean package -DskipTests
  # Check for: account-service-0.0.1-SNAPSHOT.jar
  ```

- [ ] **Gateway Service Builds Successfully**
  ```powershell
  cd C:\Users\Dhananjaya Bal\git\Event-Ledger-Repo\gateway-service
  mvn clean package -DskipTests
  # Check for: gateway-service-0.0.1-SNAPSHOT.jar
  ```

---

## 🚀 Deployment Checklist

### Step 1: Build and Start

- [ ] **Navigate to Workspace Root**
  ```powershell
  cd C:\Users\Dhananjaya Bal\git
  ```

- [ ] **Build and Start Services**
  ```powershell
  docker-compose up -d --build
  # Watch for: "Starting" and then completion
  ```

- [ ] **Wait for Build Completion**
  - Account service build: ~2-3 minutes
  - Gateway service build: ~2-3 minutes
  - Starting services: ~30 seconds
  - **Total**: ~5-7 minutes

### Step 2: Service Status Verification

- [ ] **Check Container Status**
  ```powershell
  docker-compose ps
  
  # Expected output:
  # NAME                COMMAND                  STATUS           PORTS
  # account-service     "java -jar account-..."  Up (healthy)     8080/tcp
  # gateway-service     "java -jar gateway-..."  Up (healthy)     0.0.0.0:8080->8081/tcp
  ```

- [ ] **Verify All Containers Running**
  - [ ] account-service: "Up (healthy)" ✓
  - [ ] gateway-service: "Up (healthy)" ✓

- [ ] **Check Port Mappings**
  ```powershell
  netstat -ano | findstr :8080
  # Should show listening port 8080
  ```

### Step 3: Network Verification

- [ ] **Verify Docker Network Created**
  ```powershell
  docker network ls
  # Should show: event-ledger-network
  ```

- [ ] **Check Network Configuration**
  ```powershell
  docker network inspect event-ledger-network
  # Should list both containers as connected
  ```

- [ ] **Test Internal Communication**
  ```powershell
  docker exec gateway-service curl -f http://account-service:8080/actuator/health
  # Should return: {"status":"UP"}
  ```

---

## 🔍 Functional Verification

### Gateway Service Tests

- [ ] **Service Responds to Requests**
  ```powershell
  curl -i http://localhost:8080/actuator/health
  # Expected: HTTP/1.1 200 OK
  ```

- [ ] **Swagger UI Accessible**
  ```
  Open browser: http://localhost:8080/swagger-ui.html
  # Should display API documentation
  ```

- [ ] **Health Check Endpoint**
  ```powershell
  curl http://localhost:8080/actuator/health | ConvertFrom-Json | ForEach-Object { $_.status }
  # Expected: UP
  ```

- [ ] **Metrics Endpoint**
  ```powershell
  curl http://localhost:8080/actuator/metrics
  # Should return JSON with metrics data
  ```

### Account Service Tests

- [ ] **Service Health Check (via Gateway)**
  ```powershell
  docker exec gateway-service curl http://account-service:8080/actuator/health
  # Expected: {"status":"UP"}
  ```

- [ ] **Service Logs Show Startup Success**
  ```powershell
  docker-compose logs account-service | Select-String "started"
  # Should find successful startup message
  ```

### Circuit Breaker Tests

- [ ] **Circuit Breaker Configured**
  ```powershell
  curl http://localhost:8080/actuator/health | ConvertFrom-Json | ForEach-Object { $_.components }
  # Should show circuitbreaker status
  ```

- [ ] **Account Service Monitored**
  ```powershell
  docker-compose logs gateway-service | Select-String "accountService"
  # Should show circuit breaker initialization
  ```

---

## 📝 Data Verification

### Database Verification

- [ ] **H2 Database Created (Gateway)**
  ```powershell
  docker exec gateway-service ls -la /root/.h2/
  # Should show database files (if persistent H2 used)
  ```

- [ ] **H2 Database Created (Account)**
  ```powershell
  docker exec account-service ls -la /root/.h2/
  # Should show database files (if persistent H2 used)
  ```

### Configuration Verification

- [ ] **Gateway Service Uses Docker Profile**
  ```powershell
  docker-compose logs gateway-service | Select-String "docker" -Context 2
  # Should show: Activating Spring Boot profile [docker]
  ```

- [ ] **Account Service Configuration**
  ```powershell
  docker-compose logs account-service | Select-String "application"
  # Should show successful application startup
  ```

---

## 🔒 Security Verification

- [ ] **Account Service NOT Exposed**
  ```powershell
  # Try to access from host (should fail)
  curl http://localhost:8080 -UseBasicParsing 2>&1
  # Note: Will get gateway service response, not account service
  ```

- [ ] **Internal Communication Only**
  ```powershell
  # Verify no direct mapping
  docker-compose ps | Select-String "account-service"
  # Should show: 8080/tcp (no host port mapping)
  ```

- [ ] **Gateway Exposed on Public Port**
  ```powershell
  docker-compose ps | Select-String "gateway-service"
  # Should show: 0.0.0.0:8080->8081/tcp
  ```

---

## 🐛 Troubleshooting Tests

### If Services Won't Start

1. **Check Docker Logs**
   ```powershell
   docker-compose logs --tail=50
   ```

2. **Verify Images Built**
   ```powershell
   docker images | grep -E "(account|gateway)"
   # Should show built images
   ```

3. **Force Rebuild**
   ```powershell
   docker-compose down -v
   docker-compose up -d --build
   ```

### If Gateway Can't Reach Account Service

1. **Test Network Connection**
   ```powershell
   docker exec gateway-service ping account-service
   # Should resolve to an IP address
   ```

2. **Test Service Port**
   ```powershell
   docker exec gateway-service curl -v http://account-service:8080
   # Should connect successfully
   ```

3. **Check Gateway Logs**
   ```powershell
   docker-compose logs gateway-service | Select-String "account-service\|error\|exception" -Context 2
   ```

### If Port 8080 Already In Use

1. **Find Process Using Port**
   ```powershell
   netstat -ano | findstr :8080
   ```

2. **Edit docker-compose.yml**
   - Change `"8080:8081"` to `"8081:8081"` or another port

3. **Restart Services**
   ```powershell
   docker-compose down
   docker-compose up -d --build
   ```

---

## 📊 Performance Tests

- [ ] **Response Time Acceptable**
  ```powershell
  Measure-Command { curl http://localhost:8080/actuator/health } | ForEach-Object { $_.TotalMilliseconds }
  # Expected: < 200ms
  ```

- [ ] **Memory Usage Reasonable**
  ```powershell
  docker stats --no-stream
  # Check memory usage for each container
  ```

- [ ] **CPU Usage Normal**
  ```powershell
  docker stats --no-stream
  # Both services should have low CPU usage at rest
  ```

---

## ✨ Final Validation

- [ ] **Full System Test**
  ```powershell
  # 1. Stop services
  docker-compose stop
  
  # 2. Wait 5 seconds
  Start-Sleep -Seconds 5
  
  # 3. Start services
  docker-compose up -d
  
  # 4. Wait for startup
  Start-Sleep -Seconds 30
  
  # 5. Check status
  docker-compose ps
  # All should be "Up (healthy)"
  ```

- [ ] **Access All Endpoints**
  - [ ] Gateway: `http://localhost:8080`
  - [ ] Swagger: `http://localhost:8080/swagger-ui.html`
  - [ ] Health: `http://localhost:8080/actuator/health`
  - [ ] Metrics: `http://localhost:8080/actuator/metrics`

- [ ] **Review Documentation**
  - [ ] Read DOCKER_SETUP.md
  - [ ] Review docker-compose.yml
  - [ ] Check application-docker.properties

---

## 🎉 Success Indicators

✅ **All checks passed if**:
1. Both containers show "Up (healthy)" status
2. Port 8080 is accessible from host
3. Gateway can communicate with Account service
4. All API endpoints respond correctly
5. No errors in logs
6. Services survive restart

---

## 📞 Getting Help

If validation fails:

1. **Check Logs**
   ```powershell
   docker-compose logs -f
   ```

2. **Consult DOCKER_SETUP.md**
   - Comprehensive troubleshooting section

3. **Review docker-compose.yml**
   - Verify configuration syntax

4. **Rebuild Everything**
   ```powershell
   docker-compose down -v
   docker image prune -a
   docker-compose up -d --build
   ```

---

## 📋 Sign-Off

Once all checks pass:

- [ ] Date Verified: _________
- [ ] By: _________
- [ ] Notes: _________

System is ready for development and testing!
