# 🐳 Event Ledger Docker Setup - Complete Documentation

Welcome! This directory contains a complete Docker setup for the Event Ledger microservices architecture.

## 📚 Documentation Index

### Quick Start
- **[DOCKER_QUICK_REFERENCE.md](DOCKER_QUICK_REFERENCE.md)** ⭐ START HERE
  - 3-command quick start
  - Common commands cheat sheet
  - Troubleshooting quick fixes
  - **Best for**: Developers who just want to get started

### Comprehensive Guides
- **[DOCKER_SETUP.md](DOCKER_SETUP.md)** 📖 COMPLETE GUIDE
  - Architecture overview with diagrams
  - Detailed setup instructions
  - All endpoint documentation
  - Database access instructions
  - Troubleshooting guide with solutions
  - **Best for**: Understanding the full system

### Detailed Information
- **[DOCKER_SETUP_SUMMARY.md](DOCKER_SETUP_SUMMARY.md)** 📋 DETAILED SUMMARY
  - File descriptions for all created files
  - Architecture explanation
  - Environment variables documentation
  - Production considerations
  - **Best for**: Reference and deeper understanding

### Validation & Testing
- **[DOCKER_VALIDATION.md](DOCKER_VALIDATION.md)** ✅ VERIFICATION
  - Pre-deployment checklist
  - Step-by-step validation
  - Functional tests
  - Troubleshooting tests
  - **Best for**: Verifying setup and diagnosing issues

### Tools
- **[docker-manager.ps1](docker-manager.ps1)** 🛠️ MANAGEMENT SCRIPT
  - PowerShell script for easy management
  - Interactive menu system
  - Color-coded output
  - **Usage**: `.\docker-manager.ps1 [command]`

---

## 🚀 Quick Start (30 seconds)

```powershell
# 1. Navigate to workspace
cd C:\Users\Dhananjaya Bal\git

# 2. Start all services
docker-compose up -d

# 3. Open browser
# Gateway API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

**That's it!** Your services are now running.

---

## 📦 What's Included

### Services
| Service | Status | Port | Access |
|---------|--------|------|--------|
| **Gateway Service** | Public | 8080 | 🌐 External users |
| **Account Service** | Internal | 8080 | 🔒 Gateway only |

### Docker Files
- ✅ `docker-compose.yml` - Service orchestration
- ✅ `Dockerfile` (Account Service) - Container build
- ✅ `Dockerfile` (Gateway Service) - Container build
- ✅ `.dockerignore` (Both services) - Build optimization
- ✅ `application-docker.properties` - Docker config

### Documentation
- ✅ `DOCKER_SETUP.md` - Complete guide
- ✅ `DOCKER_QUICK_REFERENCE.md` - Quick reference
- ✅ `DOCKER_SETUP_SUMMARY.md` - Detailed summary
- ✅ `DOCKER_VALIDATION.md` - Validation guide
- ✅ `README.md` - This file

---

## 🏗️ Architecture

```
                         EXTERNAL USERS
                              ↓
                    Port 8080 (Public)
                              ↓
                    ┌─────────────────┐
                    │ Gateway Service │ ← Accepts all requests
                    │ (Port 8081)     │
                    └────────┬────────┘
                             │
                  Docker Network Bridge
                  (event-ledger-network)
                             │
                    ┌────────────────────┐
                    │ Account Service    │ ← Internal only
                    │ (Port 8080)        │   No external access
                    │ Not Exposed        │
                    └────────────────────┘
```

**Key Points:**
- ✅ Gateway Service: Public, exposed on port 8080
- ✅ Account Service: Internal, only accessible from gateway
- ✅ Isolated network for secure internal communication
- ✅ Health checks ensure service availability

---

## 📋 File Structure

```
C:\Users\Dhananjaya Bal\git\
│
├── 📄 docker-compose.yml                ← Main orchestration file
├── 📖 README.md                         ← This file
├── 📖 DOCKER_SETUP.md                   ← Complete guide
├── 📖 DOCKER_QUICK_REFERENCE.md         ← Cheat sheet
├── 📖 DOCKER_SETUP_SUMMARY.md           ← Detailed info
├── ✅ DOCKER_VALIDATION.md              ← Verification guide
├── 🛠️ docker-manager.ps1                 ← Management script
├── ⚙️ .env.example                       ← Config template
│
├── Event-Ledger-Local/account-service/
│   ├── 🐳 Dockerfile
│   └── 📋 .dockerignore
│
└── Event-Ledger-Repo/gateway-service/
    ├── 🐳 Dockerfile
    ├── 📋 .dockerignore
    └── ⚙️ src/main/resources/application-docker.properties
```

---

## ✨ Key Features

✅ **Production-Ready**
- Multi-stage Docker builds for optimization
- Health checks for both services
- Proper dependency management
- Circuit breaker for resilience

✅ **Secure**
- Account service intentionally isolated
- No unnecessary external exposure
- Network-level isolation
- Security considerations documented

✅ **Developer-Friendly**
- Single command to start all services
- Comprehensive documentation
- PowerShell management script
- Swagger UI for API testing

✅ **Well-Documented**
- Multiple guides for different needs
- Complete troubleshooting section
- Validation checklist
- Quick reference guide

---

## 🎯 Common Tasks

### Start Services
```powershell
docker-compose up -d
```

### View Logs
```powershell
docker-compose logs -f
```

### Stop Services
```powershell
docker-compose stop
```

### Restart Services
```powershell
docker-compose restart
```

### Rebuild Everything
```powershell
docker-compose up -d --build
```

### Remove Everything
```powershell
docker-compose down -v
```

For more commands, see [DOCKER_QUICK_REFERENCE.md](DOCKER_QUICK_REFERENCE.md)

---

## 🌐 Access Endpoints

### Gateway Service (Public)
- **API Base**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs**: `http://localhost:8080/v3/api-docs`
- **Health**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`

### Account Service (Internal)
- **Access**: Via gateway only (Docker internal network)
- **URL**: `http://account-service:8080` (from within Docker)

---

## 🆘 Need Help?

### For Quick Answers
→ See **[DOCKER_QUICK_REFERENCE.md](DOCKER_QUICK_REFERENCE.md)**

### For Complete Setup Details
→ See **[DOCKER_SETUP.md](DOCKER_SETUP.md)**

### For File Descriptions
→ See **[DOCKER_SETUP_SUMMARY.md](DOCKER_SETUP_SUMMARY.md)**

### For Verification
→ See **[DOCKER_VALIDATION.md](DOCKER_VALIDATION.md)**

### For Troubleshooting
```powershell
# View detailed logs
docker-compose logs -f

# Check service status
docker-compose ps

# Reset everything
docker-compose down -v
docker-compose up -d --build
```

---

## 🔄 PowerShell Management Script

Use the included PowerShell script for easier management:

```powershell
# Navigate to workspace
cd C:\Users\Dhananjaya Bal\git

# Show available commands
.\docker-manager.ps1 help

# Start services
.\docker-manager.ps1 start

# Show status
.\docker-manager.ps1 status

# View logs
.\docker-manager.ps1 logs

# Restart services
.\docker-manager.ps1 restart
```

---

## ✅ Verification Checklist

After starting services, verify:

- [ ] Both containers show "Up (healthy)" in `docker-compose ps`
- [ ] Gateway responds at `http://localhost:8080/actuator/health`
- [ ] Swagger UI loads at `http://localhost:8080/swagger-ui.html`
- [ ] No errors in `docker-compose logs`

See **[DOCKER_VALIDATION.md](DOCKER_VALIDATION.md)** for complete verification steps.

---

## 📝 Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- PowerShell 5.1+ (for management script)
- 2GB available disk space

---

## 🚨 Important Notes

⚠️ **Development Setup**
- Uses H2 in-memory databases
- Data is lost when containers stop
- Not recommended for production data

⚠️ **Production Readiness**
- Before production, review "Production Considerations" in [DOCKER_SETUP_SUMMARY.md](DOCKER_SETUP_SUMMARY.md)
- Implement proper security (Spring Security)
- Use persistent databases (PostgreSQL, MySQL)
- Configure logging and monitoring
- Enable HTTPS/TLS

---

## 📞 Getting Started

### Step 1: Review Quick Reference
Read [DOCKER_QUICK_REFERENCE.md](DOCKER_QUICK_REFERENCE.md) (5 minutes)

### Step 2: Start Services
```powershell
cd C:\Users\Dhananjaya Bal\git
docker-compose up -d
```

### Step 3: Verify Everything Works
Follow [DOCKER_VALIDATION.md](DOCKER_VALIDATION.md) (10 minutes)

### Step 4: Access Services
Open browser: `http://localhost:8080/swagger-ui.html`

### Step 5: Read Full Documentation
Review [DOCKER_SETUP.md](DOCKER_SETUP.md) for complete understanding

---

## 🎉 You're All Set!

Your Event Ledger microservices are ready to go. Start with the quick reference and explore the comprehensive documentation as needed.

**Need more info?** Each markdown file is self-contained and can be read independently.

---

## 📄 Document Overview

| Document | Purpose | Length | Best For |
|----------|---------|--------|----------|
| DOCKER_QUICK_REFERENCE.md | Commands & troubleshooting | 3 min read | Quick answers |
| DOCKER_SETUP.md | Complete architecture & setup | 15 min read | Full understanding |
| DOCKER_SETUP_SUMMARY.md | File details & production | 10 min read | Reference & planning |
| DOCKER_VALIDATION.md | Testing & verification | 20 min | Verification & debugging |
| README.md | Overview & navigation | This page | Getting started |

---

**Last Updated**: July 12, 2026  
**Status**: ✅ Production-Ready Documentation  
**Version**: 1.0
