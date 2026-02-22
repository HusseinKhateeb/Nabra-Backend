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

## AVSR Fusion (Lip + Audio)

Endpoint:
- `POST /api/v1/lipreading/avsr/fuse`
- `POST /api/v1/lipreading/avsr/fuse-files` (multipart upload)

Behavior:
- Compares `audioText` with top lip predictions.
- Selects the most similar lip word as the final result.
- For `fuse-files`, backend runs audio model on uploaded audio and lip model on uploaded video, then returns the fused final word.

### One-call upload (what you asked for)

Use this endpoint when you want backend to do full AVSR pipeline:
- `POST /api/v1/lipreading/avsr/fuse-files`
- form-data fields:
  - `audioFile` (required)
  - `videoFile` (required)
  - `topK` (optional, default 5)

Example curl:
```bash
curl -X POST "http://localhost:8080/api/v1/lipreading/avsr/fuse-files" \
  -H "Authorization: Bearer <TOKEN>" \
  -F "audioFile=@D:/path/audio.m4a" \
  -F "videoFile=@D:/path/video.mp4" \
  -F "topK=5"
```

### Flutter request example
```json
{
  "audioText": "مرحبا",
  "lipTopPredictions": [
    { "word": "مرحبا", "confidence": 82.5 },
    { "word": "اهلا", "confidence": 10.0 },
    { "word": "كيفك", "confidence": 4.0 },
    { "word": "شكرا", "confidence": 2.0 },
    { "word": "مع السلامة", "confidence": 1.5 }
  ],
  "topK": 5
}
```

### Optional local model execution from backend
- If `audioText` is missing, backend can run audio model when `audioPath` is provided.
- If `lipTopPredictions` is missing, backend can run lip model when `videoPath` is provided.

Configure in `application.yml`:
- `app.ai.avsr.pythonCommand`
- `app.ai.avsr.lip.command`
- `app.ai.avsr.audio.command`

Supported template variables:
- `{python}`, `{video}`, `{audio}`, `{topK}`

Recommended lip command:
```text
{python} src/main/java/com/nabra/backend/modules/lipreading/avsrModels/3D ResNet-18/run_lip_file.py --video {video} --top-k {topK} --use-mediapipe --json-only
```

Recommended audio command:
```text
{python} src/main/java/com/nabra/backend/modules/lipreading/avsrModels/audio model/test_asr_ctc.py {audio}
```

### Webcam test now (before Flutter)

1) Start live audio model in terminal #1:
```bash
cd src/main/java/com/nabra/backend/modules/lipreading/avsrModels/audio\ model
.venv\Scripts\python.exe test_asr_ctc.py --realtime --duration 2 --output realtime_result.txt
```
- Press `p` to record short audio clips.
- Arabic text is appended to `realtime_result.txt`.

2) Start webcam AVSR fusion test in terminal #2:
```bash
cd src/main/java/com/nabra/backend/modules/lipreading/avsrModels/3D\ ResNet-18
.venv-win\Scripts\python.exe avsr_webcam_fusion_test.py --use-mediapipe --top-k 5
```
- Press `p` to capture lip frames from webcam.
- Script reads latest audio line from `../audio model/realtime_result.txt`.
- It prints top-5 lip predictions and final fused word.

3) Backend API test (single endpoint for Flutter later):
- `POST /api/v1/lipreading/avsr/fuse`
- Send either:
  - `audioText + lipTopPredictions`, or
  - `audioPath + videoPath` (backend runs local models using `app.ai.avsr.*.command`).

### One command (audio + video together)

Run one script only:

```bash
cd src/main/java/com/nabra/backend/modules/lipreading/avsrModels/3D\ ResNet-18
.venv-win\Scripts\python.exe avsr_live_one_command.py --use-mediapipe --top-k 5 --capture-seconds 2
```

How it works:
- Press `p` once.
- It records microphone audio and captures webcam lip frames at the same time.
- Runs audio model + lip model, then prints:
  - `Audio text`
  - lip top-5
  - final fused word (most similar to audio)
