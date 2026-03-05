# Quick Start Guide - Nabra Backend Authentication

## 🚀 5-Minute Setup

### 1. Database Setup

```bash
# Login to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE nabra;

# Create user (if not exists)
CREATE USER nabra WITH PASSWORD 'nabra';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE nabra TO nabra;

# Exit PostgreSQL
\q
```

### 2. Build & Run Application

```bash
# Navigate to project
cd c:\Users\SAQERpc\Desktop\seminar\backend\Nabra-Backend

# Build with Maven
mvn clean package

# Run application
mvn spring-boot:run

# OR run the JAR directly
java -jar target/nabra-backend-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
2024-01-15T10:30:00.000Z  INFO : Nabra backend started in 5.234s
2024-01-15T10:30:01.000Z  INFO : Initializing Spring DispatcherServlet 'dispatcherServlet'
...
Application started successfully!
```

### 3. Verify Server is Running

Visit: http://localhost:8080/swagger-ui.html

Should see the Swagger UI with all API endpoints.

---

## 🧪 Quick Test Flow

### Register User (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "displayName": "Test User",
    "userType": "HEARING",
    "preferredLanguage": "en"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "testuser",
  "email": "test@example.com",
  "role": "USER",
  "preferredLanguage": "en",
  "status": "ACTIVE",
  "emailVerified": false
}
```

### Login (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'
```

### Get User Profile (cURL)

```bash
# Replace TOKEN with the accessToken from login
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer TOKEN"
```

---

## 📋 API Quick Reference

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/api/v1/auth/register` | POST | ❌ | Create new account |
| `/api/v1/auth/login` | POST | ❌ | Get JWT token |
| `/api/v1/auth/me` | GET | ✅ | Get user profile |
| `/api/v1/auth/validate` | GET | ❌ | Validate token |
| `/api/v1/auth/change-password` | POST | ✅ | Change password |
| `/api/v1/auth/logout` | POST | ✅ | Logout user |

---

## 🔧 Configuration Checklist

- [ ] PostgreSQL database created
- [ ] Database user configured
- [ ] `application.yml` database URL matches
- [ ] JWT secret is secure (32+ chars in production)
- [ ] Token expiration time is appropriate
- [ ] Java 17+ is installed
- [ ] Maven is installed and in PATH

---

## 📊 Database Tables Auto-Created

The application uses Hibernate with `ddl-auto: update`, which automatically creates/updates tables:

- **users** - User accounts with credentials
- **user_blocks** - User blocking relationships

No manual SQL needed! Tables are created on first run.

---

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| Database connection refused | Check PostgreSQL running, verify connection string |
| Port 8080 already in use | Change `server.port` in `application.yml` |
| JWT token invalid | Ensure same JWT secret is used, token not expired |
| Username already exists | Use different username for registration |
| Password too short | Use password with at least 8 characters |

---

## 📝 Example: Programmatic Login

```java
// In your controller or service
@Autowired
private AuthService authService;

public void login() {
  AuthDtos.LoginRequest req = new AuthDtos.LoginRequest("testuser", "TestPass123!");
  AuthDtos.AuthResponse response = authService.login(req);
  
  String token = response.accessToken();
  String userId = response.userId();
  System.out.println("Logged in as: " + userId);
}
```

---

## 🔑 JWT Token Claims

Every JWT token contains:

```json
{
  "sub": "user-id",
  "username": "john_doe",
  "role": "USER",
  "iat": 1704067200,
  "exp": 1704153600
}
```

- `sub` - User ID (subject)
- `username` - Username
- `role` - User role (USER or ADMIN)
- `iat` - Issued at (timestamp)
- `exp` - Expires at (timestamp)

---

## 📞 Getting Help

1. **Check logs:** Look in console for detailed error messages
2. **Swagger UI:** Visit http://localhost:8080/swagger-ui.html to test endpoints
3. **Documentation:** Read `AUTHENTICATION_GUIDE.md` for detailed info
4. **Postman Collection:** Import `Nabra_Auth_API.postman_collection.json`

---

## ✅ You're Ready!

Your authentication system is now:
- ✅ Running locally
- ✅ Ready for development
- ✅ Ready for integration testing
- ✅ Ready for production deployment

**Next Steps:**
1. Test all endpoints with Postman
2. Integrate authentication into other modules
3. Add more user management features
4. Deploy to production with proper secrets

---

**Happy Coding! 🚀**
