# Docker Quick Reference - Event Ledger

## 🚀 Quick Start (3 Commands)

```powershell
# 1. Navigate to workspace
cd C:\Users\Dhananjaya Bal\git

# 2. Start all services
docker-compose up -d

# 3. Check status
docker-compose ps
```

---

## 📋 Common Commands

| Command | Purpose |
|---------|---------|
| `docker-compose up -d` | Start all services (background) |
| `docker-compose up -d --build` | Rebuild and start |
| `docker-compose stop` | Stop all services |
| `docker-compose start` | Start stopped services |
| `docker-compose down` | Stop and remove containers |
| `docker-compose logs -f` | View all logs (live) |
| `docker-compose logs gateway-service` | View gateway logs |
| `docker-compose logs account-service` | View account logs |
| `docker-compose ps` | Show running containers |
| `docker-compose restart` | Restart all services |

---

## 🌐 Access Endpoints

| Service | Endpoint | Access | Purpose |
|---------|----------|--------|---------|
| Gateway | `localhost:8080` | ✅ Public | API Entry Point |
| Gateway Swagger | `localhost:8080/swagger-ui.html` | ✅ Public | API Documentation |
| Gateway Health | `localhost:8080/actuator/health` | ✅ Public | Health Check |
| Account | `localhost:8080` (internal) | ❌ Internal Only | Account Service |

---

## 🔧 Troubleshooting

```powershell
# Check logs
docker-compose logs -f

# Check service health
docker-compose ps

# Rebuild services
docker-compose up -d --build

# Force restart
docker-compose restart

# Reset everything
docker-compose down -v
docker-compose up -d --build
```

---

## 📁 Key Files

| File | Location | Purpose |
|------|----------|---------|
| docker-compose.yml | `git/` | Main orchestration |
| Dockerfile | `account-service/` | Account Service build |
| Dockerfile | `gateway-service/` | Gateway Service build |
| application-docker.properties | `gateway-service/src/main/resources/` | Docker config |
| DOCKER_SETUP.md | `git/` | Full documentation |

---

## 🎯 Architecture

```
User → localhost:8080 → Gateway Service (Port 8081)
                            ↓
                      (Docker Network)
                            ↓
                      Account Service (Port 8080)
                      ⚠️ Not exposed to host
```

**Gateway Service**: Public (Exposed on port 8080)
**Account Service**: Internal (Not exposed, only accessible from gateway)

---

## ✅ Service Status Indicators

```powershell
docker-compose ps

# Expected output:
CONTAINER ID   IMAGE              STATUS
xxx            account-service    Up (healthy)  ✅
yyy            gateway-service    Up (healthy)  ✅
```

---

## 🔐 Security Highlights

✅ Account Service **NOT exposed** to internet
✅ Only Gateway Service accepts user requests
✅ Internal communication via Docker network
✅ Health checks ensure service availability

---

## 🛑 Common Issues & Quick Fixes

### Port 8080 in use?
```powershell
# Edit docker-compose.yml:
# Change: "8080:8081"  →  "8081:8081"
docker-compose up -d
```

### Services not starting?
```powershell
# Full rebuild
docker-compose down -v
docker-compose up -d --build
docker-compose logs
```

### Gateway can't reach Account?
```powershell
# Check network
docker network inspect event-ledger-network

# Check logs
docker-compose logs gateway-service
```

---

## 📊 Monitoring

```powershell
# Real-time logs
docker-compose logs -f

# Gateway metrics
curl http://localhost:8080/actuator/metrics

# Circuit breaker status
curl http://localhost:8080/actuator/health
```

---

## 🆘 Emergency Reset

```powershell
# Nuclear option - start fresh
docker-compose down -v
docker image rm account-service gateway-service
docker-compose up -d --build
```

---

## 📝 Database Access

### H2 Console (Gateway Service)
- URL: `http://localhost:8080/h2-console`
- Database: eventdb
- Username: sa
- Password: (empty)

### H2 Console (Account Service)
- ⚠️ Not accessible from host (internal only)
- Accessible from within container only

---

## 🚨 Important Notes

⚠️ **Data Persistence**: H2 in-memory databases lose data when containers stop

⚠️ **Production Use**: Enable security, use persistent databases

⚠️ **Account Service**: Intentionally hidden from external access

---

## 📞 Help

For detailed information, see:
- `DOCKER_SETUP.md` - Complete guide
- `DOCKER_SETUP_SUMMARY.md` - Detailed summary
- `docker-compose.yml` - Configuration details

