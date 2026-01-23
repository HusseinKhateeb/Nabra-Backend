# Backend Setup & Testing Guide

## Quick Start

### 1. Prerequisites
- Java 17 or higher
- PostgreSQL running (default: localhost:5432)
- Maven 3.6+

### 2. Database Setup

Create PostgreSQL database:
```sql
CREATE DATABASE nabra;
```

Update connection in `src/main/resources/application.yml` if needed:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nabra
    username: postgres
    password: MST  # Change this!
```

### 3. Start the Backend

```bash
cd Nabra-Backend
mvn spring-boot:run
```

The API will be available at: **http://localhost:8080**

---

## Testing Login & Register

### Using cURL

#### Register a new user
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "displayName": "Test User",
    "userType": "LEARNER"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'
```

#### Validate Token
```bash
curl -X GET "http://localhost:8080/api/v1/auth/validate?token=YOUR_ACCESS_TOKEN"
```

---

## Postman Collection

A Postman collection is included: `Nabra_Auth_API.postman_collection.json`

**To import:**
1. Open Postman
2. Click "Import" → "Upload Files"
3. Select `Nabra_Auth_API.postman_collection.json`
4. Configure the `base_url` variable (default: `http://localhost:8080`)

---

## Swagger Documentation

Access interactive API docs:
```
http://localhost:8080/swagger-ui.html
```

View API specification:
```
http://localhost:8080/v3/api-docs
```

---

## API Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

## Frontend Integration Checklist

✅ **Backend Ready For:**
- User registration with validation
- User login with JWT token generation
- Token-based authentication
- CORS enabled for frontend connections
- Comprehensive error handling
- OpenAPI/Swagger documentation

**To integrate with your frontend:**
1. Call `POST /api/v1/auth/register` for new users
2. Call `POST /api/v1/auth/login` for existing users
3. Store the returned `accessToken` in your frontend
4. Include token in all API requests: `Authorization: Bearer {token}`
5. See `FRONTEND_INTEGRATION_GUIDE.md` for API endpoint details

---

## Environment Variables (Optional)

You can override settings via environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/nabra
export DB_USER=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your-secret-key-min-32-chars
export JWT_EXPIRATION=86400

mvn spring-boot:run
```

---

## Troubleshooting

**Port already in use (8080)?**
```bash
# Change port in application.yml
server:
  port: 8081
```

**Database connection error?**
- Ensure PostgreSQL is running
- Check credentials in `application.yml`
- Verify database `nabra` exists

**CORS error from frontend?**
- Update `CorsConfig.java` with your frontend URL
- Check browser console for detailed error message

**JWT token errors?**
- Ensure token is in `Authorization: Bearer TOKEN` format
- Check token expiration time
- Verify JWT secret is the same in backend

---

## Build for Production

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/nabra-backend-0.0.1-SNAPSHOT.jar

# Or with environment variables
java -jar target/nabra-backend-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:postgresql://prod-db:5432/nabra \
  --app.security.jwt.secret=your-production-secret
```

---

## Project Structure

```
src/main/java/com/nabra/backend/
├── config/              # Spring configurations
│   ├── CorsConfig.java  # CORS settings for frontend
│   ├── SecurityConfig.java
│   └── OpenApiConfig.java
├── modules/
│   └── usermanagement/
│       ├── controller/
│       │   └── AuthController.java   # Login/Register endpoints
│       ├── service/
│       │   └── AuthService.java      # Business logic
│       ├── dto/
│       │   └── AuthDtos.java         # Request/Response models
│       ├── model/
│       │   └── User.java             # User entity
│       └── repository/
│           └── UserRepository.java   # Database access
├── security/
│   ├── jwt/
│   │   ├── JwtService.java           # Token generation
│   │   └── JwtAuthenticationFilter.java
│   └── principal/
│       └── DbUserDetailsService.java
└── common/              # Shared utilities
    ├── exception/       # Error handling
    ├── model/           # Shared models
    └── web/             # Web utilities
```

---

## Support

See `FRONTEND_INTEGRATION_GUIDE.md` for detailed API integration examples.
