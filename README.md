# AI-Powered Network Configuration Compliance and Drift Detection System

A full-stack Network Management System (NMS) that automatically monitors configuration compliance of network devices using NETCONF, RESTCONF, YANG models, deterministic rule-based compliance engine, and AI-assisted analysis.

---

## 🎯 Project Overview

This system enables network administrators to:

1. Register and manage network devices
2. Define approved configuration policies (with structured rules)
3. Retrieve device configurations via **NETCONF**, **RESTCONF**, or **Mock** providers
4. Compare actual configurations against approved policies deterministically
5. Detect **configuration drift** automatically
6. Calculate **compliance scores**
7. Receive **AI-powered explanations and recommendations**
8. Manage everything from a centralized **React dashboard**

> **Engineering Principle:** Compliance is always determined by the deterministic rule engine. AI provides explanations and recommendations only — never compliance decisions.

---

## ✨ Features

| Feature | Status |
|---------|--------|
| JWT Authentication (Admin / Network Admin) | ✅ |
| Device Management (CRUD + Test Connection) | ✅ |
| Configuration Policy Management | ✅ |
| YANG Model Management | ✅ |
| NETCONF Client Integration | ✅ |
| RESTCONF Client Integration | ✅ |
| Mock Device Provider (for local dev) | ✅ |
| Deterministic Compliance Engine | ✅ |
| Drift Detection & Event Tracking | ✅ |
| Compliance Score Calculation | ✅ |
| Severity Classification (CRITICAL/HIGH/MEDIUM/LOW) | ✅ |
| Configuration History | ✅ |
| Side-by-side Configuration Diff | ✅ |
| AI Violation Analysis (Gemini/OpenAI) | ✅ |
| AI Natural Language Policy Assistant | ✅ |
| Scheduled Compliance Scanning | ✅ |
| Drift Simulation (Demo Feature) | ✅ |
| Real-time Dashboard with Charts | ✅ |
| Alert System | ✅ |
| Docker Support | ✅ |
| GNS3 Integration Guide | ✅ |

---

## 🏗️ Architecture

```
React Dashboard (Port 5173)
        ↓ REST API
Spring Boot Backend (Port 8080)
        ↓                    ↓
   PostgreSQL         Network Devices
                        ↓
              NETCONF / RESTCONF / Mock
                        ↓
              Normalized Configuration
                        ↓
             Compliance Engine (deterministic)
                        ↓
               AI Service (Port 8000)
                        ↓
                  Gemini / OpenAI
```

### Compliance Data Flow

```
Device → Provider → Normalized Config → Policy → Rule Engine → Score
                                                                  ↓
                                                          AI Analysis
                                                          (explanations only)
                                                                  ↓
                                                            Dashboard
```

---

## 🛠️ Technology Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, Recharts, Lucide React |
| Backend | Java 21, Spring Boot 3.2, Spring Data JPA, Spring Security |
| Database | PostgreSQL 15 |
| AI Service | Python 3.11, FastAPI, Pydantic v2 |
| LLM | Google Gemini / OpenAI GPT-4o-mini |
| Network | NETCONF (SSH/port 830), RESTCONF (HTTPS), YANG models |
| Auth | JWT (JJWT 0.11.5), BCrypt |
| Container | Docker, Docker Compose |

---

## 📁 Folder Structure

```
network-compliance-system/
├── frontend/                    ← React + TypeScript + Vite
│   ├── src/
│   │   ├── components/          ← Reusable UI components
│   │   ├── pages/               ← Route pages (Dashboard, Devices, etc.)
│   │   ├── services/            ← API clients (Axios)
│   │   ├── context/             ← Auth context
│   │   ├── types/               ← TypeScript interfaces
│   │   └── charts/              ← Recharts chart components
│   ├── Dockerfile
│   └── nginx.conf
├── backend/                     ← Spring Boot Java 21
│   └── src/main/java/com/nms/compliance/
│       ├── controller/          ← REST endpoints
│       ├── service/             ← Business logic
│       ├── repository/          ← JPA repositories
│       ├── entity/              ← Database entities
│       ├── dto/                 ← Data Transfer Objects
│       ├── network/             ← NETCONF/RESTCONF/Mock providers
│       ├── compliance/          ← Rule engine
│       └── security/            ← JWT authentication
├── ai-service/                  ← Python FastAPI
│   └── app/
│       ├── routes/              ← API endpoints
│       ├── services/            ← LLM client
│       ├── models/              ← Pydantic schemas
│       └── prompts/             ← Prompt templates
├── database/
│   ├── schema/001_create_schema.sql
│   └── seed/seed_demo_data.sql
├── network/
│   ├── yang/                    ← YANG model files
│   ├── mock/                    ← Mock device registry
│   └── gns3/                    ← GNS3 setup guide
├── docker-compose.yml
└── README.md
```

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# 1. Clone the repository
git clone <repo-url>
cd network-compliance-system

# 2. Create environment file
cp .env.example .env
# Edit .env and set GEMINI_API_KEY or OPENAI_API_KEY

# 3. Start all services
docker compose up --build

# 4. Access the application
# Frontend:  http://localhost:80
# Backend:   http://localhost:8080
# AI Service: http://localhost:8000
# API Docs:  http://localhost:8080/swagger-ui.html
```

### Option 2: Local Development

#### Prerequisites
- Java 21+
- Maven 3.9+
- Node.js 20+
- Python 3.11+
- PostgreSQL 15+

#### 1. Database Setup

```bash
# Create database
psql -U postgres -c "CREATE USER nms_user WITH PASSWORD 'nms_password';"
psql -U postgres -c "CREATE DATABASE nms_compliance OWNER nms_user;"

# Run schema
psql -U nms_user -d nms_compliance -f database/schema/001_create_schema.sql

# Seed demo data
psql -U nms_user -d nms_compliance -f database/seed/seed_demo_data.sql
```

#### 2. Backend Setup

```bash
cd backend

# Copy and configure environment
cp .env.example .env
# Edit .env with your values

# Run the application
./mvnw spring-boot:run

# Backend runs at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

#### 3. AI Service Setup

```bash
cd ai-service

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Copy and configure environment
cp .env.example .env
# Set GEMINI_API_KEY=your-key or OPENAI_API_KEY=your-key

# Run the service
uvicorn app.main:app --reload --port 8000

# AI Service runs at http://localhost:8000
# API Docs: http://localhost:8000/docs
```

#### 4. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Copy and configure environment
cp .env.example .env
# VITE_API_BASE_URL=http://localhost:8080/api

# Run development server
npm run dev

# Frontend runs at http://localhost:5173
```

---

## 🔑 Default Credentials

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| netadmin | admin123 | NETWORK_ADMIN |

---

## 📡 API Reference

### Authentication
```
POST /api/auth/login
Body: { "username": "admin", "password": "admin123" }
```

### Devices
```
GET    /api/devices
POST   /api/devices
GET    /api/devices/{id}
PUT    /api/devices/{id}
DELETE /api/devices/{id}
POST   /api/devices/{id}/test-connection
GET    /api/devices/{id}/configuration
POST   /api/devices/{id}/configuration/retrieve
POST   /api/devices/{id}/scan
GET    /api/devices/{id}/compliance
GET    /api/devices/{id}/drift
GET    /api/devices/{id}/history
POST   /api/devices/{id}/simulate-drift
POST   /api/devices/{id}/reset-drift
```

### Policies
```
GET    /api/policies
POST   /api/policies
GET    /api/policies/{id}
PUT    /api/policies/{id}
DELETE /api/policies/{id}
```

### Compliance
```
POST /api/compliance/scan-all
GET  /api/compliance/results
GET  /api/compliance/scans/{scanId}
```

### Drift Events
```
GET   /api/drift
PATCH /api/drift/{id}/status
```

### Dashboard
```
GET /api/dashboard/summary
GET /api/dashboard/trends
```

### AI Analysis
```
POST /api/ai/analyze
GET  /api/ai/analyses
GET  /api/ai/analyses/{deviceId}
```

### YANG Models
```
GET    /api/yang-models
POST   /api/yang-models
GET    /api/yang-models/{id}
DELETE /api/yang-models/{id}
POST   /api/yang-models/{id}/validate
```

---

## 🎮 Demo Workflow

### Step 1: Login
Navigate to http://localhost:5173 → Login with `admin / admin123`

### Step 2: View Dashboard
See 5 demo devices, compliance scores, and drift events.

### Step 3: Run Compliance Scan
Go to Devices → Select Router-01 → Click "Run Compliance Scan"

### Step 4: View Results
See compliance score, violations, and AI-generated recommendations.

### Step 5: Simulate Drift
Go to Devices → Select Router-01 → Click "Simulate Drift"
- Telnet changes from disabled → enabled (CRITICAL)
- NTP changes from enabled → disabled (MEDIUM)

### Step 6: Run Scan Again
Compliance drops from 100% → 75%
New drift events appear in the Drift Events page.

### Step 7: View AI Analysis
Go to AI Insights page to see AI-generated explanations and recommendations.

### Step 8: View History
Go to History page, select Router-01 to see the compliance trend over time.

---

## 📊 Compliance Score Thresholds

| Score | Status |
|-------|--------|
| 100% | ✅ Fully Compliant |
| 80-99% | 🔵 Minor Drift |
| 50-79% | 🟠 Major Drift |
| < 50% | 🔴 Critical Non-Compliant |

---

## 🤖 AI Architecture

The AI service is intentionally separated from the compliance engine:

```
Compliance Engine (Java) → Deterministic score
                        ↓
              AI Service (Python FastAPI)
                        ↓
              Only receives: violations list, device type, score
              Never receives: passwords, credentials, raw config
                        ↓
              Returns: summary, analysis, recommendations
              Does NOT change: compliance score, severity levels
```

---

## 🔒 Security Considerations

- Device passwords are **AES-encrypted** at rest in the database
- **JWT tokens** expire after 24 hours (configurable)
- **Passwords are never logged** or sent to AI service
- **CORS** is configured to allow only the frontend origin
- **Input validation** on all API endpoints
- **Environment variables** for all secrets (no hardcoded credentials)

---

## 🧪 Testing

```bash
# Backend tests
cd backend
./mvnw test

# Key test cases:
# - ComplianceEngineTest: All rules pass, 3/5 pass, EQUALS/CONTAINS/GREATER_THAN operators
# - MockDeviceProviderTest: All 5 demo devices return expected configurations
# - PolicyServiceTest: Policy CRUD operations
# - DeviceServiceTest: Connection test, configuration retrieval
```

---

## 🌐 NETCONF/RESTCONF Integration

See `network/gns3/GNS3_SETUP.md` for instructions on:
1. Setting up GNS3 topology
2. Enabling NETCONF on Cisco IOS-XE (port 830)
3. Enabling RESTCONF on Cisco IOS-XE (HTTPS)
4. Registering real devices in the NMS
5. Troubleshooting connection issues

---

## 🔧 YANG Model Support

YANG models are stored in `network/yang/`:
- `network-device.yang` — Router compliance model
- `switch-config.yang` — Switch compliance model

Upload YANG models via the **YANG Models** page in the dashboard.
Validation uses `pyang` if installed:

```bash
pip install pyang
pyang network/yang/network-device.yang
```

---

## 🐳 Docker Services

| Service | Port | Description |
|---------|------|-------------|
| frontend | 80 | React app served by Nginx |
| backend | 8080 | Spring Boot REST API |
| ai-service | 8000 | Python FastAPI AI service |
| postgres | 5432 | PostgreSQL database |

---

## 🗺️ Future Enhancements

- [ ] WebSocket real-time drift notifications
- [ ] Email/Slack alert notifications
- [ ] Remediation command execution (with approval workflow)
- [ ] Multi-vendor YANG model library
- [ ] SNMP trap integration
- [ ] Role-based access control (RBAC) per device group
- [ ] Configuration version control (Git-based)
- [ ] Compliance report generation (PDF)
- [ ] Network topology visualization
- [ ] OpenConfig YANG model support

---

## 📝 License

MIT License — For educational purposes.

---

*Built as a college-level Network Management System project demonstrating YANG, NETCONF, RESTCONF, Configuration Compliance, Drift Detection, REST APIs, and AI-assisted Network Management.*
