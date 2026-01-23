# Nabra Backend - Authentication & Security Implementation Guide

## 📋 Overview

This document provides a comprehensive guide to the authentication and security system implemented in the Nabra Backend application. The system uses **JWT (JSON Web Tokens)** for stateless authentication with **PostgreSQL** for data persistence.

---

## 🔑 Key Features

### Authentication & Authorization
- ✅ **User Registration** with validation (username, email, password)
- ✅ **User Login** with JWT token generation
- ✅ **JWT Token Management** with expiration handling
- ✅ **Role-Based Access Control (RBAC)** - USER and ADMIN roles
- ✅ **Password Change** functionality with current password validation
- ✅ **Account Status Management** (ACTIVE, SUSPENDED, INACTIVE, DELETED)
- ✅ **Email Verification** flag (extensible for actual email verification)
- ✅ **Last Login Tracking** for audit purposes

### Security Features
- ✅ **BCrypt Password Hashing** (cost 12) for secure password storage
- ✅ **Stateless Session Management** - No server-side sessions
- ✅ **CSRF Protection Disabled** - Not needed for stateless JWT API
- ✅ **Method-Level Security** with @PreAuthorize annotations
- ✅ **Custom Exception Handling** with standardized error responses
- ✅ **Comprehensive Input Validation** with Jakarta validation
- ✅ **Logging & Audit Trail** for security events

### API Design
- ✅ **RESTful Endpoints** following best practices
- ✅ **OpenAPI/Swagger Documentation** with JWT authentication
- ✅ **Standardized Error Responses** in JSON format
- ✅ **Request/Response DTOs** for clean API contracts

---

## 🏗️ Architecture

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.3.4 |
| Security | Spring Security | 3.3.4 |
| JWT Library | JJWT | 0.11.5 |
| Database | PostgreSQL | Latest |
| ORM | Spring Data JPA/Hibernate | 3.3.4 |
| API Documentation | Springdoc OpenAPI | 2.6.0 |
| Build Tool | Maven | 4.0.0 |

### Project Structure

```
src/main/java/com/nabra/backend/
├── common/
│   ├── exception/
│   │   ├── ApiError.java              # Standard error response format
│   │   └── GlobalExceptionHandler.java # Global exception handling
│   ├── model/
│   │   ├── BaseEntity.java            # Base entity with audit fields
│   │   └── Enums.java                 # Application enumerations
│   └── web/
│       └── SecurityUtils.java         # Security utility methods
├── config/
│   ├── SecurityConfig.java            # Spring Security configuration
│   └── OpenApiConfig.java             # Swagger/OpenAPI configuration
├── modules/
│   └── usermanagement/
│       ├── controller/
│       │   └── AuthController.java    # Authentication REST endpoints
│       ├── service/
│       │   └── AuthService.java       # Authentication business logic
│       ├── repository/
│       │   └── UserRepository.java    # User data access
│       ├── model/
│       │   └── User.java              # User entity with JPA mappings
│       ├── dto/
│       │   ├── AuthDtos.java          # Authentication request/response DTOs
│       │   └── UserDtos.java          # User profile DTOs
│       └── exception/
│           ├── UserAlreadyExistsException.java
│           ├── InvalidCredentialsException.java
│           └── UserNotFoundException.java
└── security/
    ├── jwt/
    │   ├── JwtService.java            # JWT token generation and validation
    │   └── JwtAuthenticationFilter.java # JWT authentication filter
    └── principal/
        ├── UserPrincipal.java         # Spring Security UserDetails implementation
        └── DbUserDetailsService.java  # Database-backed user details service
```

---

## 📊 Database Schema

### Users Table

```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    full_name VARCHAR(100),
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    user_type VARCHAR(20) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login TIMESTAMP,
    gender VARCHAR(20),
    age INTEGER,
    avatar_url VARCHAR(400),
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'ar',
    high_contrast_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    font_scale DOUBLE NOT NULL DEFAULT 1.0,
    vibration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE INDEX idx_users_username (username),
    UNIQUE INDEX idx_users_email (email)
);

CREATE TABLE user_blocks (
    blocker_id VARCHAR(36) NOT NULL,
    blocked_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id),
    FOREIGN KEY (blocker_id) REFERENCES users(id),
    FOREIGN KEY (blocked_id) REFERENCES users(id)
);
```

### Entity Relationships

- **User extends BaseEntity** - Inherits `id` (UUID), `createdAt`, `updatedAt`
- **User.blockedUsers** - Many-to-many self-referential relationship for blocking functionality
- **Indexes** - On username and email for fast lookups during authentication

---

## 🔐 Authentication Flow

### User Registration Flow

```
1. Client sends POST /api/v1/auth/register with user details
   ↓
2. AuthService validates:
   - Username uniqueness
   - Email uniqueness
   - Password strength
   - Input validation
   ↓
3. Password is hashed using BCrypt (cost 12)
   ↓
4. User entity is created and persisted to database
   ↓
5. JWT token is generated with user claims
   ↓
6. AuthResponse returned with token and user info
```

### User Login Flow

```
1. Client sends POST /api/v1/auth/login with username/password
   ↓
2. AuthenticationManager validates credentials:
   - Retrieves user from database
   - Compares password hash using BCrypt
   ↓
3. Account status is verified:
   - Check if ACTIVE (not SUSPENDED, INACTIVE, DELETED)
   ↓
4. Last login timestamp is updated
   ↓
5. JWT token is generated with user claims
   ↓
6. AuthResponse returned with token and user info
```

### JWT Token Structure

```
Header: { "alg": "HS256", "typ": "JWT" }

Payload: {
  "sub": "user-id-uuid",
  "username": "username",
  "role": "USER",
  "iat": 1234567890,
  "exp": 1234654290
}

Signature: HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

### Request Authentication Flow

```
1. Client includes JWT in Authorization header:
   Authorization: Bearer <JWT_TOKEN>
   ↓
2. JwtAuthenticationFilter intercepts request
   ↓
3. Token is extracted and validated using JwtService
   ↓
4. Claims are parsed and user is loaded from database
   ↓
5. UserPrincipal is created and set in SecurityContext
   ↓
6. Request proceeds with authenticated context
```

---

## 📡 REST API Endpoints

### 1. User Registration

**Endpoint:** `POST /api/v1/auth/register`

**Access:** Public (no authentication required)

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe",
  "userType": "HEARING",
  "preferredLanguage": "en"
}
```

**Validation Rules:**
- `username`: 3-50 characters, alphanumeric (unique)
- `email`: valid email format (unique)
- `password`: minimum 8 characters
- `displayName`: 2-80 characters
- `userType`: DEAF | MUTE | HEARING | INSTRUCTOR | OTHER
- `preferredLanguage`: optional, defaults to 'ar'

**Success Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "preferredLanguage": "en",
  "status": "ACTIVE",
  "emailVerified": false
}
```

**Error Responses:**
- `400 Bad Request` - Validation failed
- `409 Conflict` - Username or email already exists

---

### 2. User Login

**Endpoint:** `POST /api/v1/auth/login`

**Access:** Public (no authentication required)

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "SecurePass123!"
}
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "preferredLanguage": "en",
  "status": "ACTIVE",
  "emailVerified": false
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid credentials
- `404 Not Found` - Account suspended, inactive, or deleted

---

### 3. Get Current User Profile

**Endpoint:** `GET /api/v1/auth/me`

**Access:** Protected (requires JWT token)

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com",
  "displayName": "John Doe",
  "role": "USER",
  "status": "ACTIVE",
  "userType": "HEARING",
  "preferredLanguage": "en",
  "emailVerified": false,
  "highContrastEnabled": false,
  "fontScale": 1.0,
  "vibrationEnabled": true,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

---

### 4. Change Password

**Endpoint:** `POST /api/v1/auth/change-password`

**Access:** Protected (requires JWT token)

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "currentPassword": "SecurePass123!",
  "newPassword": "NewSecurePass456!",
  "confirmPassword": "NewSecurePass456!"
}
```

**Validation Rules:**
- `currentPassword`: must match user's actual current password
- `newPassword`: minimum 8 characters, must match confirmPassword
- `confirmPassword`: must match newPassword

**Success Response (200 OK):**
```json
"Password changed successfully"
```

**Error Responses:**
- `400 Bad Request` - Passwords don't match or too short
- `401 Unauthorized` - Current password incorrect
- `404 Not Found` - User not found

---

### 5. Validate Token

**Endpoint:** `GET /api/v1/auth/validate?token=<JWT_TOKEN>`

**Access:** Public (no authentication required)

**Query Parameters:**
- `token` (required): JWT token to validate

**Success Response (200 OK):**
```
true
```

**Invalid Token Response (200 OK):**
```
false
```

---

### 6. Logout User

**Endpoint:** `POST /api/v1/auth/logout`

**Access:** Protected (requires JWT token)

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Success Response (200 OK):**
```json
"Logged out successfully. Please discard your token."
```

**Note:** In a stateless JWT system, logout is primarily client-side (discard token). This endpoint exists for audit logging purposes.

---

## 🧪 Testing with Postman

### Setting Up Postman

1. **Import Collection:**
   - Open Postman
   - Click "Import"
   - Select `Nabra_Auth_API.postman_collection.json`
   - Collection will be imported with all endpoints

2. **Configure Variables:**
   - Open collection and go to "Variables" tab
   - Set `base_url`: `http://localhost:8080`
   - After login, copy the JWT token from response
   - Set `auth_token` variable with the token value

### Testing Workflow

#### Step 1: Register a User
```bash
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "TestPass123!",
  "displayName": "Test User",
  "userType": "HEARING",
  "preferredLanguage": "en"
}
```

**Expected:** 201 Created with JWT token

#### Step 2: Login with Credentials
```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "TestPass123!"
}
```

**Expected:** 200 OK with JWT token

#### Step 3: Get User Profile
```bash
GET http://localhost:8080/api/v1/auth/me
Authorization: Bearer <JWT_TOKEN_FROM_STEP_2>
```

**Expected:** 200 OK with user profile

#### Step 4: Change Password
```bash
POST http://localhost:8080/api/v1/auth/change-password
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "currentPassword": "TestPass123!",
  "newPassword": "NewPass456!",
  "confirmPassword": "NewPass456!"
}
```

**Expected:** 200 OK with success message

#### Step 5: Login with New Password
```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "NewPass456!"
}
```

**Expected:** 200 OK with JWT token

#### Step 6: Logout
```bash
POST http://localhost:8080/api/v1/auth/logout
Authorization: Bearer <JWT_TOKEN>
```

**Expected:** 200 OK with logout message

---

## ⚙️ Configuration

### application.yml

```yaml
spring:
  application:
    name: nabra-backend
  
  datasource:
    url: jdbc:postgresql://localhost:5432/nabra
    username: nabra
    password: nabra
  
  jpa:
    hibernate:
      ddl-auto: update  # use 'validate' in production
    properties:
      hibernate:
        format_sql: true
  
  jackson:
    time-zone: UTC

server:
  port: 8080

app:
  security:
    jwt:
      # IMPORTANT: Change this to a strong random secret in production
      # Minimum 32 characters recommended
      secret: "your-super-secret-key-change-this-in-production"
      # Token expiration in seconds (86400 = 24 hours)
      expirationSeconds: 86400

logging:
  level:
    root: INFO
    com.nabra.backend: DEBUG
    org.springframework.security: DEBUG
```

### Environment Setup

**Development:**
```bash
# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nabra
export SPRING_DATASOURCE_USERNAME=nabra
export SPRING_DATASOURCE_PASSWORD=nabra
export APP_SECURITY_JWT_SECRET=dev-secret-key-change-in-production
```

**Production:**
```bash
# Use strong secrets from secure vault
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db-host:5432/nabra
export SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
export SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
export APP_SECURITY_JWT_SECRET=${JWT_SECRET_FROM_VAULT}
```

---

## 🚀 Running the Application

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Maven 3.6+

### Build
```bash
mvn clean package
```

### Run
```bash
# Development
mvn spring-boot:run

# Production
java -jar target/nabra-backend-0.0.1-SNAPSHOT.jar
```

### Access Points
- **API Base URL:** http://localhost:8080/api/v1
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI Docs:** http://localhost:8080/v3/api-docs
- **Health Check:** http://localhost:8080/actuator/health

---

## 🔒 Security Best Practices

### 1. Password Security
- ✅ Passwords hashed with BCrypt (cost 12)
- ✅ Never stored or logged in plain text
- ✅ Minimum 8 characters required
- ✅ Input validation on registration and change

### 2. JWT Token Security
- ✅ Token expires after 24 hours (configurable)
- ✅ Signed with HS256 algorithm
- ✅ Secret key must be 32+ characters in production
- ✅ Tokens transmitted over HTTPS in production

### 3. Database Security
- ✅ Username and email fields have unique constraints
- ✅ Passwords never logged or exposed in error messages
- ✅ Audit fields track creation and modification time
- ✅ User status prevents compromised accounts

### 4. API Security
- ✅ All protected endpoints require valid JWT
- ✅ CSRF protection disabled (not needed for stateless JWT)
- ✅ Session management is stateless
- ✅ Comprehensive input validation
- ✅ Standardized error responses (no sensitive data leaked)

### 5. Infrastructure Security
- ✅ Use HTTPS in production (TLS 1.3+)
- ✅ Set secure JWT secret in environment variables
- ✅ Enable database encryption at rest
- ✅ Implement rate limiting on auth endpoints
- ✅ Monitor login attempts and failed authentications

---

## 📝 Code Examples

### Using Authentication in Controllers

```java
@RestController
@RequestMapping("/api/v1/protected")
@RequiredArgsConstructor
public class ProtectedController {

  @GetMapping("/resource")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<String> getProtectedResource() {
    String userId = SecurityUtils.getCurrentUserId();
    String username = SecurityUtils.getCurrentUsername();
    User user = SecurityUtils.getCurrentUser();
    
    return ResponseEntity.ok("Resource for " + username);
  }

  @DeleteMapping("/resource/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<String> deleteResource(@PathVariable String id) {
    // Only ADMIN users can access this
    return ResponseEntity.ok("Deleted resource " + id);
  }

  @PostMapping("/resource")
  @PreAuthorize("isAuthenticated() && hasRole('USER')")
  public ResponseEntity<String> createResource() {
    User currentUser = SecurityUtils.getCurrentUser();
    // Create resource for current user
    return ResponseEntity.ok("Resource created");
  }
}
```

### Extending Authorization

```java
// Custom authorization annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') || @userService.isOwner(#id)")
public @interface RequireOwnerOrAdmin {
}

// Using custom annotation
@DeleteMapping("/users/{id}")
@RequireOwnerOrAdmin
public ResponseEntity<String> deleteUser(@PathVariable String id) {
  // Implementation
}
```

---

## 🐛 Troubleshooting

### Token Expired
**Error:** "JWT signature does not match"
**Solution:** Login again to get a fresh token

### Invalid Credentials
**Error:** "Invalid username or password"
**Solution:** 
- Verify username/email case sensitivity
- Check password is correct
- Ensure account is ACTIVE

### User Blocked/Suspended
**Error:** "Your account has been suspended"
**Solution:** Contact system administrator to reactivate account

### Database Connection Issues
**Error:** "Cannot get a connection"
**Solution:**
- Verify PostgreSQL is running
- Check connection string in application.yml
- Verify database user permissions

---

## 🔄 Future Enhancements

1. **Email Verification**
   - Send verification link on registration
   - Verify email before account activation

2. **Refresh Tokens**
   - Implement refresh token rotation
   - Sliding window token expiration

3. **Two-Factor Authentication (2FA)**
   - TOTP-based authentication
   - SMS-based verification

4. **OAuth2/OpenID Connect**
   - Social login (Google, GitHub, etc.)
   - External provider integration

5. **Rate Limiting**
   - Prevent brute force attacks
   - Limit login attempts per IP

6. **Account Recovery**
   - Password reset flow
   - Email-based account recovery

7. **Audit Logging**
   - Track all authentication events
   - Login/logout timestamps
   - Failed login attempts

---

## 📚 References

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://tools.ietf.org/html/rfc7519)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Spring Boot Security Guide](https://spring.io/guides/topical/spring-security-architecture/)
- [PostgreSQL Security](https://www.postgresql.org/docs/current/sql-syntax.html)

---

## 📞 Support & Contact

For issues or questions regarding authentication:
1. Check the troubleshooting section
2. Review the code comments and documentation
3. Contact the development team
4. Submit issues on the project repository

---

**Last Updated:** January 2025
**Author:** Nabra Development Team
**License:** [Your License Here]
