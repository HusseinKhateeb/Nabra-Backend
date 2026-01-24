# 🧪 Testing Profile & Settings Endpoints

## استخدام CURL للاختبار

### 1️⃣ Register (تسجيل حساب جديد)
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ahmad",
    "email": "ahmad@example.com",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!",
    "displayName": "أحمد محمد"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "ahmad",
  "email": "ahmad@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "emailVerified": false
}
```

---

### 2️⃣ Login (تسجيل دخول)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ahmad@example.com",
    "password": "SecurePass123!"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "ahmad",
  "email": "ahmad@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "emailVerified": false
}
```

**احفظ الـ token في متغير:**
```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 3️⃣ Get Profile (الملف الشخصي)
```bash
curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "ahmad",
  "displayName": "أحمد محمد",
  "email": "ahmad@example.com",
  "phoneNumber": null,
  "joinDate": "2024-01-15T10:30:00Z",
  "role": "USER",
  "status": "ACTIVE",
  "userType": "OTHER",
  "gender": null,
  "age": null,
  "avatarUrl": null,
  "highContrastEnabled": false,
  "fontScale": 1.0,
  "vibrationEnabled": true,
  "emailVerified": true,
  "statistics": {
    "transfers": 0,
    "hoursOfUse": 0,
    "accuracy": 0.0
  }
}
```

---

### 4️⃣ Update Profile (تحديث الملف الشخصي)
```bash
curl -X PUT http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "أحمد محمد جديد",
    "gender": "male",
    "age": 25,
    "phoneNumber": "+972598765432",
    "avatarUrl": "https://example.com/avatar.jpg",
    "highContrastEnabled": false,
    "fontScale": 1.2,
    "vibrationEnabled": true
  }'
```

**Response:** نفس Profile response مع البيانات المحدثة

---

### 5️⃣ Get Settings (الإعدادات)
```bash
curl -X GET http://localhost:8080/api/v1/users/settings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "preferredLanguage": "ar",
  "highContrastEnabled": false,
  "fontScale": 1.0,
  "vibrationEnabled": true,
  "publicProfile": true,
  "showContactInfo": false,
  "allowNotifications": true,
  "allowDataCollection": true,
  "dataCollectionLevel": "basic"
}
```

---

### 6️⃣ Update Settings (تحديث الإعدادات)
```bash
curl -X PUT http://localhost:8080/api/v1/users/settings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "preferredLanguage": "ar",
    "highContrastEnabled": true,
    "fontScale": 1.5,
    "vibrationEnabled": false,
    "publicProfile": true,
    "showContactInfo": true,
    "allowNotifications": true,
    "allowDataCollection": false,
    "dataCollectionLevel": "minimal"
  }'
```

**Response:** نفس Settings response مع البيانات المحدثة

---

## 🔐 Windows PowerShell Version

```powershell
# Register
$registerResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/auth/register" `
  -Method POST `
  -ContentType "application/json" `
  -Body @"
{
  "username": "ahmad",
  "email": "ahmad@example.com",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!",
  "displayName": "أحمد محمد"
}
"@

# Get Token from response
$token = ($registerResponse.Content | ConvertFrom-Json).token

# Get Profile
$headers = @{"Authorization" = "Bearer $token"}
$profileResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/users/profile" `
  -Method GET `
  -Headers $headers `
  -ContentType "application/json"

$profileResponse.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

---

## 📊 Curl Commands with Variables

```bash
#!/bin/bash

# Variables
BASE_URL="http://localhost:8080"
EMAIL="ahmad@example.com"
PASSWORD="SecurePass123!"

# 1. Register
echo "=== Register ==="
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"ahmad\",
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\",
    \"confirmPassword\": \"$PASSWORD\",
    \"displayName\": \"أحمد محمد\"
  }")

TOKEN=$(echo $REGISTER_RESPONSE | jq -r '.token')
echo "Token: $TOKEN"

# 2. Get Profile
echo -e "\n=== Get Profile ==="
curl -s -X GET "$BASE_URL/api/v1/users/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

# 3. Update Settings
echo -e "\n=== Update Settings ==="
curl -s -X PUT "$BASE_URL/api/v1/users/settings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "preferredLanguage": "ar",
    "highContrastEnabled": true,
    "fontScale": 1.5,
    "vibrationEnabled": false,
    "publicProfile": true,
    "allowNotifications": true,
    "allowDataCollection": false,
    "dataCollectionLevel": "minimal"
  }' | jq '.'

# 4. Get Updated Settings
echo -e "\n=== Get Updated Settings ==="
curl -s -X GET "$BASE_URL/api/v1/users/settings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'
```

---

## ✅ Expected Status Codes

| Endpoint | Method | Success Status | Error Status |
|----------|--------|---------------|----|
| /auth/register | POST | 201 Created | 400/409 |
| /auth/login | POST | 200 OK | 401 |
| /users/profile | GET | 200 OK | 401/404 |
| /users/profile | PUT | 200 OK | 400/401/404 |
| /users/settings | GET | 200 OK | 401/404 |
| /users/settings | PUT | 200 OK | 400/401/404 |

---

## 🐛 Error Responses

### 400 - Bad Request
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users/settings"
}
```

### 401 - Unauthorized
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid JWT token",
  "path": "/api/v1/users/profile"
}
```

### 404 - Not Found
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found",
  "path": "/api/v1/users/profile"
}
```

---

## 🎯 Tips

1. **احفظ الـ Token**
   ```bash
   TOKEN=$(curl -s ... | jq -r '.token')
   ```

2. **استخدم jq لـ Pretty Print**
   ```bash
   curl -s ... | jq '.'
   ```

3. **اختبر مع Postman**
   - استيراد الـ Collection من `Nabra_Profile_Settings.postman_collection.json`
   - أسهل من CURL!

4. **تصحيح الأخطاء**
   - إذا فشل الـ Token: تحقق من صحة الـ JWT
   - إذا كان 404: تأكد من User ID
   - إذا كان 401: تأكد من تضمين الـ Authorization header
