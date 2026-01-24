# 🎯 Endpoints Reference - Profile & Settings

## انسخ هذه الـ URLs للـ Postman

### 1️⃣ Get Profile
```
GET http://localhost:8080/api/v1/users/profile
Authorization: Bearer <JWT_TOKEN>
```

**Response Example:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "ahmad",
  "displayName": "أحمد محمد",
  "email": "ahmad@example.com",
  "phoneNumber": "+972598765432",
  "joinDate": "2024-01-15T10:30:00Z",
  "role": "USER",
  "status": "ACTIVE",
  "userType": "OTHER",
  "gender": "male",
  "age": 25,
  "avatarUrl": null,
  "highContrastEnabled": false,
  "fontScale": 1.0,
  "vibrationEnabled": true,
  "emailVerified": true,
  "statistics": {
    "transfers": 247,
    "hoursOfUse": 12,
    "accuracy": 98.0
  }
}
```

---

### 2️⃣ Update Profile
```
PUT http://localhost:8080/api/v1/users/profile
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "displayName": "أحمد محمد جديد",
  "gender": "male",
  "age": 25,
  "phoneNumber": "+972598765432",
  "avatarUrl": "https://example.com/avatar.jpg",
  "highContrastEnabled": false,
  "fontScale": 1.2,
  "vibrationEnabled": true
}
```

**Response:** نفس Profile response أعلى

---

### 3️⃣ Get Settings
```
GET http://localhost:8080/api/v1/users/settings
Authorization: Bearer <JWT_TOKEN>
```

**Response Example:**
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

### 4️⃣ Update Settings
```
PUT http://localhost:8080/api/v1/users/settings
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "preferredLanguage": "ar",
  "highContrastEnabled": true,
  "fontScale": 1.5,
  "vibrationEnabled": false,
  "publicProfile": true,
  "showContactInfo": true,
  "allowNotifications": true,
  "allowDataCollection": false,
  "dataCollectionLevel": "minimal"
}
```

**Response:** نفس Settings response أعلى

---

## 📋 Summary Table

| الـ Endpoint | Method | URL | Description |
|-----------|--------|-----|-------------|
| Profile | GET | `/api/v1/users/profile` | جلب الملف الشخصي |
| Profile | PUT | `/api/v1/users/profile` | تحديث الملف الشخصي |
| Settings | GET | `/api/v1/users/settings` | جلب الإعدادات |
| Settings | PUT | `/api/v1/users/settings` | تحديث الإعدادات |

---

## 🔑 Authentication

جميع الـ Endpoints تحتاج JWT Token في Header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 📊 Statistics Fields

| Field | Value | Source |
|-------|-------|--------|
| transfers | 247 | COUNT(*) FROM sessions |
| hoursOfUse | 12 | COUNT(*) FROM chats |
| accuracy | 98.0 | User entity field |

---

## ⚙️ Settings Fields Details

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| preferredLanguage | String | "ar" | اللغة المفضلة |
| highContrastEnabled | Boolean | false | الألوان العالية التباين |
| fontScale | Double | 1.0 | حجم الخط |
| vibrationEnabled | Boolean | true | الاهتزاز |
| publicProfile | Boolean | true | ملف عام |
| showContactInfo | Boolean | false | عرض الهاتف |
| allowNotifications | Boolean | true | الإشعارات |
| allowDataCollection | Boolean | true | جمع البيانات |
| dataCollectionLevel | String | "basic" | مستوى جمع البيانات |

---

## 🧪 Quick Test Steps

1. **سجل حساب جديد**
   ```
   POST /api/v1/auth/register
   ```

2. **سجل دخول**
   ```
   POST /api/v1/auth/login
   ```
   - انسخ الـ token

3. **اختبر الـ Endpoints**
   ```
   GET /api/v1/users/profile
   GET /api/v1/users/settings
   PUT /api/v1/users/settings
   ```

---

**جاهز للاستخدام! 🚀**
