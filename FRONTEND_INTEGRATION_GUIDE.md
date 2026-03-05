# Frontend Integration Guide

This guide explains how to integrate your frontend with the Nabra Backend API.

## Overview

The backend is now configured with:
- ✅ JWT-based authentication
- ✅ User registration and login endpoints
- ✅ CORS enabled for frontend connections
- ✅ Comprehensive error handling
- ✅ OpenAPI/Swagger documentation

## API Base URL

```
http://localhost:8080/api/v1
```

## Authentication Endpoints

### 1. **Register a New User**

**Endpoint:** `POST /auth/register`

**Request:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe",
  "userType": "LEARNER"
}
```

**Valid User Types:**
- `LEARNER` - For regular learners
- `INSTRUCTOR` - For instructors
- `ADMIN` - For administrators

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "username": "john_doe",
  "email": "john@example.com",
  "role": "LEARNER",
  "status": "ACTIVE",
  "emailVerified": false
}
```

---

### 2. **Login**

**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "username": "john_doe",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "username": "john_doe",
  "email": "john@example.com",
  "role": "LEARNER",
  "status": "ACTIVE",
  "emailVerified": false
}
```

---

### 3. **Validate Token**

**Endpoint:** `GET /auth/validate?token=YOUR_TOKEN`

**Response (200 OK):**
```json
{
  "valid": true,
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "username": "john_doe"
}
```

---

### 4. **Change Password** (Authenticated)

**Endpoint:** `POST /auth/change-password`

**Headers:**
```
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Request:**
```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewPass456!",
  "confirmPassword": "NewPass456!"
}
```

**Response (200 OK):**
```
Password changed successfully
```

---

## Using JWT Tokens

### Storing the Token

After login/register, store the `accessToken` in your frontend:
- **Local Storage** (simple, but less secure)
- **Session Storage** (cleared when browser closes)
- **HTTP-only Cookies** (more secure, but requires backend configuration)

### Sending Authenticated Requests

Include the token in the `Authorization` header for protected endpoints:

```javascript
const headers = {
  "Authorization": "Bearer " + accessToken,
  "Content-Type": "application/json"
};

fetch("http://localhost:8080/api/v1/users/profile", {
  method: "GET",
  headers: headers
});
```

---

## CORS Configuration

The backend is configured to accept requests from:
- `http://localhost:3000` (React default)
- `http://localhost:3001` (Alternative)
- `http://localhost:4200` (Angular default)

**To add more origins**, update `CorsConfig.java`:

```java
registry.addMapping("/api/**")
    .allowedOrigins("http://your-frontend-url.com")
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
    .allowedHeaders("*")
    .allowCredentials(true)
    .maxAge(3600);
```

---

## Error Responses

The API returns consistent error responses:

```json
{
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "status": 401,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Common Error Codes:
- **400** - Bad Request (validation error)
- **401** - Unauthorized (invalid credentials or missing token)
- **403** - Forbidden (insufficient permissions)
- **409** - Conflict (username/email already exists)
- **500** - Internal Server Error

---

## Integration Steps

1. **Store the JWT token** from login/register response
2. **Include the token** in all API requests: `Authorization: Bearer {accessToken}`
3. **Handle token expiration** - if you get a 401 response, token has expired
4. **Use the userId** from the response to identify the user in your frontend

---

## API Documentation

Access the interactive Swagger UI documentation:

```
http://localhost:8080/swagger-ui.html
```

All API endpoints are documented with:
- Request/Response schemas
- Example payloads
- Error descriptions
- Authorization requirements

---

## Running the Backend

```bash
# Build and run
mvn clean install
mvn spring-boot:run

# The API will be available at: http://localhost:8080
```

---

## Environment Setup

### Required
- Java 17+
- PostgreSQL (default config uses `jdbc:postgresql://localhost:5432/nabra`)
- Maven 3.6+

### Database Connection
Update `application.yml` if using different database:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-host:5432/your-db
    username: your-user
    password: your-pass
```

---

## Security Notes

⚠️ **Important for Production:**

1. **JWT Secret** - Update `app.security.jwt.secret` in `application.yml` with a long, random secret (min 32 chars)
2. **CORS Origins** - Update allowed origins in `CorsConfig.java` and `application.yml`
3. **Token Expiration** - Adjust `app.security.jwt.expirationSeconds` as needed
4. **HTTPS** - Use HTTPS in production
5. **Database** - Use strong credentials and secure connections

---

## Support

For issues or questions about the API, check:
1. Server logs at `http://localhost:8080/actuator/health`
2. Swagger documentation at `http://localhost:8080/swagger-ui.html`
3. Backend source code in `/src/main/java/com/nabra/backend/modules/usermanagement/`
