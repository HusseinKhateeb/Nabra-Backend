# Nabra Backend (Spring Boot Scaffold)

This is a **starter backend scaffold** for the Nabra project based on the provided SRS.

## Tech
- Java 17
- Spring Boot 3.x
- PostgreSQL
- JWT Auth (Bearer)
- Swagger / OpenAPI (springdoc)

## Quickstart

### 1) Run Postgres
```bash
docker compose up -d
```

### 2) Run the API
```bash
mvn spring-boot:run
```

### 3) Swagger UI
Open:
- `/swagger-ui.html`

## Auth
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

Use the returned `accessToken` as:
```
Authorization: Bearer <TOKEN>
```

## Core Modules (Packages)
- `LipReadingModule` → `com.nabra.backend.modules.lipreading`
- `SessionHistoryModule` → `com.nabra.backend.modules.sessionhistory`
- `ChatCommunicationModule` → `com.nabra.backend.modules.chatcommunication`
- `SmartPredictionModule` → `com.nabra.backend.modules.smartprediction`
- `VisualDictionaryModule` → `com.nabra.backend.modules.visualdictionary`
- `UserManagementModule` → `com.nabra.backend.modules.usermanagement`
- `AdminToolsModule` → `com.nabra.backend.modules.admintools`

## ER Entities (JPA)
- User
- Session
- Chat
- Message
- LearningProgress
- DictionaryEntry
- Report

## Notes
- Lip reading, STT, and smart prediction endpoints are implemented as **optional** integrations to external AI services.
  Configure base URLs in `application.yml` under `app.ai.*`.
- `spring.jpa.hibernate.ddl-auto=update` is for development scaffolding. For production, use migrations.
