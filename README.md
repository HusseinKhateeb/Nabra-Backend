# Nabra — Backend

> Spring Boot REST API powering the Nabra Arabic Audio-Visual Speech Recognition app — handles authentication, session management, lip-reading dictionary, and AVSR inference via an embedded Python model.

---

## 🏗️ Architecture Overview

```
Flutter App
    │
    ▼ HTTP + JWT
Spring Boot API
    ├── Auth Module        → Register, Login, JWT issuance
    ├── User Module        → Profiles, settings
    ├── AVSR Module        → Triggers Python AVSR model, stores results
    ├── Session History    → Past recognition sessions per user
    └── Dictionary Module  → Arabic lip-reading reference entries
         │
         ├──► PostgreSQL (Neon)     → Persistent data
         └──► Python AVSR Script   → 3D ResNet-18 + Whisper inference
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot |
| Auth | JWT (JSON Web Tokens) |
| Database | PostgreSQL (hosted on Neon) |
| AI Inference | Python, PyTorch, 3D ResNet-18, MediaPipe, OpenAI Whisper |
| Containerization | Docker |
| Build Tool | Maven |

---

## 📋 Prerequisites

Make sure you have the following installed:

- **Java 17+**
- **Maven**
- **Python 3.9+** with the following packages:
  ```
  torch
  mediapipe
  openai-whisper
  opencv-python
  numpy
  ```
- **Docker** (optional, for containerized run)
- A **Neon** account with a PostgreSQL database created → [neon.tech](https://neon.tech)

---

## ⚙️ Environment Setup

The project reads configuration from a `.env` file at the root of the repository. This file is **not committed to version control** — you must create it manually.

### 1. Create your `.env` file

```env
# Database (Neon PostgreSQL)
DB_URL=jdbc:postgresql://<your-neon-host>/<your-db-name>?sslmode=require
DB_USERNAME=your_neon_username
DB_PASSWORD=your_neon_password

# JWT
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRATION_MS=86400000

# Server
SERVER_PORT=8080

# Python / AVSR Model (cross-platform)
PYTHON_PATH=python
AVSR_SCRIPT_PATH=src/main/java/com/nabra/backend/modules/lipreading/avsrModels
```

### 2. Windows users — create `.env-win`

If you're on Windows, create a separate `.env-win` file for Windows-specific paths:

```env
# Windows-specific overrides
PYTHON_PATH=C:\Users\YourName\AppData\Local\Programs\Python\Python39\python.exe
AVSR_SCRIPT_PATH=src\main\java\com\nabra\backend\modules\lipreading\avsrModels
FFMPEG_PATH=C:\ffmpeg\bin\ffmpeg.exe
```

> **Note:** `.env-win` requires a Windows-specific launch config to be picked up. The default Spring Boot launch profile reads from `.env` only.

### 3. Add AVSR model checkpoints

The model weights are excluded from version control. Place your trained model files here:

```
src/main/java/com/nabra/backend/modules/lipreading/avsrModels/
└── 3D ResNet-18/
    └── checkpoints/
        ├── your_model.pth     ← place here
        └── your_model.pt      ← place here
```

---

## 🚀 Running the Project

### Option A — Run locally with Maven

```bash
# 1. Clone the repo
git clone https://github.com/HusseinKhateeb/Nabra-Backend
cd Nabra-Backend

# 2. Install Python dependencies
pip install torch mediapipe openai-whisper opencv-python numpy

# 3. Run the Spring Boot app
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

---

### Option B — Run with Docker

```bash
# Build and start
docker-compose up --build

# Stop
docker-compose down
```

> Make sure your `.env` file is present at the root before running Docker — the container reads from it at startup.

---

## 📡 API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT token |

### AVSR
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/avsr/recognize` | Submit video for AVSR inference |
| GET | `/api/avsr/history` | Get session history for current user |

### Dictionary
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/dictionary` | Get all lip-reading dictionary entries |
| GET | `/api/dictionary/search?q=` | Search dictionary by keyword |

### User
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/user/profile` | Get current user profile |
| PUT | `/api/user/profile` | Update user profile |

> All endpoints except `/api/auth/**` require a valid JWT token in the `Authorization: Bearer <token>` header.

---

## 🗄️ Database

This project uses **PostgreSQL hosted on [Neon](https://neon.tech)** — a serverless Postgres platform.

To set up:
1. Create a free account at [neon.tech](https://neon.tech)
2. Create a new project and database
3. Copy the connection string into your `.env` as `DB_URL`

The schema is managed by Spring Boot's auto-DDL — tables are created automatically on first run.

---

## 📁 Project Structure

```
src/main/java/com/nabra/backend/
├── modules/
│   ├── auth/              → Registration, login, JWT filter
│   ├── user/              → User profiles
│   ├── avsr/              → AVSR session handling, Python bridge
│   ├── lipreading/
│   │   └── avsrModels/
│   │       ├── 3D ResNet-18/   → Lip-reading model + checkpoints
│   │       └── audio model/    → Whisper audio inference script
│   └── dictionary/        → Lip-reading dictionary entries
├── config/                → Security config, JWT config, CORS
└── NabraApplication.java  → Entry point
```

---

## 🔗 Related

- [Nabra Frontend (Flutter)](https://github.com/HusseinKhateeb/Nabra-Frontend)
- [Nabra Main Repo](https://github.com/HusseinKhateeb/Nabra)

---

## 👤 Author

**Hussein Khateeb**
[GitHub](https://github.com/HusseinKhateeb) · [LinkedIn](https://linkedin.com/in/hussein-khateeb-33464a352)
