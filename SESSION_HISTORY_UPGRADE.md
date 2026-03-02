# Session History Module - Production-Ready Upgrade

## Summary of Changes

This upgrade transforms the Session History module to support ALL modules (lip-reading + chat + voice-to-text + learning) and makes it production-ready with proper error handling, transactions, and advanced filtering.

### ✅ Step 1: Core Data Model (Entity & Database)

**Updated Session Entity** [Session.java](src/main/java/com/nabra/backend/modules/sessionhistory/model/Session.java)
- Added `sessionType` (LIP_READING, CHAT, VOICE_TO_TEXT, LEARNING) - required field
- Added `content` - main text/transcript/summary for any module type
- Added `contentRefId` - optional reference to message/chat/learning record
- Added `deviceInfo` - device/browser/OS information
- Added `modelVersion` - algorithm version used
- Added `isOffline` - boolean flag for offline processing
- Added indexes on `sessionType` and `status` for fast filtering
- Kept all legacy fields (resultText, resultAudioUrl, accuracyScore) for backwards compatibility

**New Enum** [Enums.java](src/main/java/com/nabra/backend/common/model/Enums.java)
- `SessionType` enum with values: LIP_READING, CHAT, VOICE_TO_TEXT, LEARNING

**Database Migration** [V2__Add_Session_MultiModule_Support.sql](src/main/resources/db/migration/V2__Add_Session_MultiModule_Support.sql)
- Adds all new columns with proper types and defaults
- Creates indexes for `sessionType` and `status`
- Includes SQL comments for documentation

### ✅ Step 2: Data Transfer Objects (DTOs)

**Enhanced SessionDtos** [SessionDtos.java](src/main/java/com/nabra/backend/modules/sessionhistory/dto/SessionDtos.java)

**StartSessionRequest** - now includes:
- `sessionType` (required) - which module type
- `inputType` (required)
- `outputType` (required)
- `title` (optional)
- `contentPreview` (optional)
- `deviceInfo` (optional)
- `modelVersion` (optional)
- `isOffline` (optional)

**StopSessionRequest** - now includes:
- `content` - final content/transcript
- `contentRefId` - reference to source record
- `resultText`, `resultAudioUrl`, `accuracyScore` (legacy fields, still supported)

**SessionResponse** - now includes all new fields for complete visibility

### ✅ Step 3: Custom Exception Classes

Created three new custom exceptions in exception package:
- [NotFoundException.java](src/main/java/com/nabra/backend/common/exception/NotFoundException.java)
- [ForbiddenException.java](src/main/java/com/nabra/backend/common/exception/ForbiddenException.java)
- [BadRequestException.java](src/main/java/com/nabra/backend/common/exception/BadRequestException.java)

Updated [GlobalExceptionHandler.java](src/main/java/com/nabra/backend/common/exception/GlobalExceptionHandler.java) to handle all three new exceptions with proper HTTP status codes (404, 403, 400)

### ✅ Step 4: Service Layer Improvements

**Enhanced SessionService** [SessionService.java](src/main/java/com/nabra/backend/modules/sessionhistory/service/SessionService.java)

**Key Improvements:**
1. **Transactional Operations** - All write operations marked with `@Transactional`
2. **Better Exception Handling** - Replaced IllegalArgumentException with proper custom exceptions
3. **Safe Enum Parsing** - All enum filters now safely parse and throw BadRequestException on invalid values
4. **Enhanced Filters** - list() method now supports:
   - `sessionType` filter (LIP_READING|CHAT|VOICE_TO_TEXT|LEARNING)
   - `status` filter (ACTIVE|COMPLETED|FAILED)
   - `outputType` filter (TEXT|VOICE)
   - `keyword` search in both `content` AND `resultText` (previously only resultText)
   - `minDuration` / `maxDuration` filters in seconds
   - Date range filtering (from/to)

5. **New Operations:**
   - `getById()` - fetch single session with authorization check
   - `deleteById()` - delete single session with authorization check
   - `deleteAllUserSessions()` - delete all user history (privacy feature)

6. **Authorization** - Every operation validates that user owns the session

### ✅ Step 5: Controller Layer Enhancements

**Enhanced SessionController** [SessionController.java](src/main/java/com/nabra/backend/modules/sessionhistory/controller/SessionController.java)

**Existing Endpoints:**
- `POST /api/v1/sessions` - start session
- `POST /api/v1/sessions/{sessionId}/stop` - stop session
- `GET /api/v1/sessions` - list with filters

**New Endpoints:**
- `GET /api/v1/sessions/{sessionId}` - view single session
- `DELETE /api/v1/sessions/{sessionId}` - delete single session
- `DELETE /api/v1/sessions?confirm=true` - delete all user sessions (requires confirmation)

**Improved Filtering:**
All filter parameters are optional and include OpenAPI documentation:
- `from` - from date (ISO instant)
- `to` - to date (ISO instant)
- `sessionType` - LIP_READING|CHAT|VOICE_TO_TEXT|LEARNING
- `status` - ACTIVE|COMPLETED|FAILED
- `outputType` - TEXT|VOICE
- `keyword` - search in content
- `minDuration` - minimum duration in seconds
- `maxDuration` - maximum duration in seconds
- `pageable` - standard Spring pagination/sorting

### 📋 API Usage Examples

**1. Start a new lip-reading session:**
```bash
POST /api/v1/sessions
{
  "sessionType": "LIP_READING",
  "inputType": "LIVE",
  "outputType": "TEXT",
  "deviceInfo": "Chrome/120.0 on Windows",
  "modelVersion": "v2.1.0",
  "isOffline": false
}
```

**2. Start a chat session:**
```bash
POST /api/v1/sessions
{
  "sessionType": "CHAT",
  "inputType": "LIVE",
  "outputType": "TEXT",
  "title": "Discussion with Instructor",
  "contentPreview": "Initial message"
}
```

**3. Stop a session:**
```bash
POST /api/v1/sessions/{sessionId}/stop
{
  "content": "Full transcript here",
  "resultText": "Recognized text",
  "accuracyScore": 0.95
}
```

**4. Get all chat sessions from past month:**
```bash
GET /api/v1/sessions?sessionType=CHAT&from=2026-01-25T00:00:00Z&to=2026-02-25T23:59:59Z&size=20&sort=startedAt,desc
```

**5. Search sessions by keyword:**
```bash
GET /api/v1/sessions?keyword=arabic&minDuration=30&maxDuration=300
```

**6. View specific session:**
```bash
GET /api/v1/sessions/{sessionId}
```

**7. Delete specific session:**
```bash
DELETE /api/v1/sessions/{sessionId}
```

**8. Delete all user sessions (privacy):**
```bash
DELETE /api/v1/sessions?confirm=true
```

### 🔐 Security Features

✅ All endpoints require authentication (checked via SecurityUtils.currentPrincipal())
✅ User authorization enforced - users can only access their own sessions
✅ Transaction isolation prevents race conditions
✅ Safe enum parsing prevents injection attacks

### 📊 Database Schema

```sql
CREATE TABLE sessions (
  id VARCHAR(255) PRIMARY KEY,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  user_id VARCHAR(255) NOT NULL REFERENCES users(id),
  session_type VARCHAR(20) NOT NULL DEFAULT 'LIP_READING',
  input_type VARCHAR(20) NOT NULL,
  output_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  ended_at TIMESTAMP,
  duration_seconds BIGINT,
  content VARCHAR(5000),
  content_ref_id VARCHAR(100),
  result_text VARCHAR(2000),
  result_audio_url VARCHAR(400),
  accuracy_score DOUBLE,
  device_info VARCHAR(500),
  model_version VARCHAR(50),
  is_offline BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_sessions_user_started ON sessions(user_id, started_at);
CREATE INDEX idx_sessions_output_type ON sessions(output_type);
CREATE INDEX idx_sessions_type ON sessions(session_type);
CREATE INDEX idx_sessions_status ON sessions(status);
```

### 🚀 Benefits

1. **Multi-Module Support** - One table stores history for all features
2. **Production Ready** - Proper error handling, transactions, security
3. **Advanced Filtering** - Powerful search with 8+ filter options
4. **Backwards Compatible** - Legacy fields still work (resultText, resultAudioUrl)
5. **Privacy Compliant** - Support for bulk deletion per SRS requirement
6. **Performance** - Indexes on common filter fields
7. **Developer Experience** - Clear DTOs, strong typing, comprehensive validation
