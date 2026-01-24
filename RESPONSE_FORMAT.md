# 📱 Response Format Examples

## صفحة 1: الملف الشخصي (Profile Page)

### صورة الصفحة:
```
┌─────────────────────────────┐
│        12:00 ⚡ 📶 🔋       │
├─────────────────────────────┤
│  ← الملف الشخصي            │
├─────────────────────────────┤
│                             │
│        [Avatar Image]       │
│       أحمد محمد             │
│      مصر في 2024            │
│                             │
├─────────────────────────────┤
│  المعلومات الشخصية:        │
│  ┌───────────────────────┐  │
│  │ الاسم الكامل:         │  │
│  │ أحمد محمد             │  │
│  │                       │  │
│  │ البريد الإلكتروني:    │  │
│  │ ahmad@gmail.com       │  │
│  │                       │  │
│  │ رقم الهاتف:           │  │
│  │ +972598765432         │  │
│  │                       │  │
│  │ العمر:                │  │
│  │ 25 سنة                │  │
│  └───────────────────────┘  │
│                             │
├─────────────────────────────┤
│  الإحصائيات:                │
│  ┌──────────────────────┐   │
│  │ 247  |  12  |  98%  │   │
│  │الجلسات│المحادثات│النجاح│  │
│  └──────────────────────┘   │
└─────────────────────────────┘
```

### JSON Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "ahmad",
  "displayName": "أحمد محمد",
  "email": "ahmad@gmail.com",
  "phoneNumber": "+972598765432",
  "joinDate": "2024-01-10T08:30:00Z",
  "role": "USER",
  "status": "ACTIVE",
  "userType": "OTHER",
  "gender": "male",
  "age": 25,
  "avatarUrl": "https://example.com/avatars/ahmad.jpg",
  "highContrastEnabled": false,
  "fontScale": 1.0,
  "vibrationEnabled": true,
  "emailVerified": true,
  "statistics": {
    "transfers": 247,        ✅ عدد الجلسات (محسوب من DB)
    "hoursOfUse": 12,        ✅ عدد المحادثات (محسوب من DB)
    "accuracy": 98.0         ✅ نسبة النجاح
  }
}
```

### Endpoint:
```
GET /api/v1/users/profile
Authorization: Bearer <JWT_TOKEN>
```

---

## صفحة 2: الإعدادات (Settings Page)

### صورة الصفحة:
```
┌─────────────────────────────┐
│        12:00 ⚡ 📶 🔋       │
├─────────────────────────────┤
│  ← الإعدادات                 │
├─────────────────────────────┤
│  الإعدادات العامة:          │
│  ┌─[████████████]─────┐     │
│  │ الإعلانات                 │
│  ├─────────────────────┤     │
│  │ الإعلانات الموجهة         │
│  │ اسمح للإعلانات المخصصة    │
│  │ ┌─[████████████]─────┐    │
│  │ │ المحتوى المخصص        │  │
│  │ │ دوريات بحثية           │  │
│  │ └─────────────────────┘    │
│  └─[████████████]─────┐       │
│  │ حفظ التفضيلات           │ │
│  └─────────────────────┘     │
│                             │
├─────────────────────────────┤
│  الخصوصية والأمان:         │
│  ┌──────────────┐            │
│  │ ┌─[███]─┐   │            │
│  │ │ملفي عام │   │            │
│  │ └────────┘   │            │
│  │              │            │
│  │ ┌─[   ]─┐   │            │
│  │ │اظهر الهاتف │   │            │
│  │ └────────┘   │            │
│  │              │            │
│  │ ┌─[███]─┐   │            │
│  │ │الإشعارات │   │            │
│  │ └────────┘   │            │
│  │              │            │
│  │ ┌─[   ]─┐   │            │
│  │ │جمع البيانات│   │            │
│  │ │ ┌─────────────┐ │            │
│  │ │ │اختر المستوى │ │            │
│  │ │ │حفظ التفضيلات│ │            │
│  │ │ └─────────────┘ │            │
│  │ └────────┘   │            │
│  └──────────────┘            │
└─────────────────────────────┘
```

### JSON Response:
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
  "allowDataCollection": false,
  "dataCollectionLevel": "minimal"
}
```

### Endpoints:

#### Get Settings:
```
GET /api/v1/users/settings
Authorization: Bearer <JWT_TOKEN>
```

#### Update Settings:
```
PUT /api/v1/users/settings
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

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

---

## 🔢 شرح الأرقام في الإحصائيات

| الرقم | المعنى | من أين يأتي |
|-----|--------|-----------|
| **247** | عدد الجلسات | `SELECT COUNT(*) FROM sessions WHERE user_id = ?` |
| **12** | عدد المحادثات | `SELECT COUNT(*) FROM chats WHERE user_id IN (SELECT chat_id FROM chat_participants WHERE user_id = ?)` |
| **98.0** | نسبة النجاح/التقدم | من حقل `accuracy` في جدول `users` |

---

## ✅ Test Cases لـ Postman

### 1️⃣ Test Get Profile
```bash
GET http://localhost:8080/api/v1/users/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Expected Status:** 200 OK
**Response Time:** < 500ms

---

### 2️⃣ Test Get Settings
```bash
GET http://localhost:8080/api/v1/users/settings
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Expected Status:** 200 OK

---

### 3️⃣ Test Update Settings
```bash
PUT http://localhost:8080/api/v1/users/settings
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "preferredLanguage": "ar",
  "highContrastEnabled": true,
  "vibrationEnabled": false,
  "publicProfile": true,
  "allowNotifications": false
}
```

**Expected Status:** 200 OK
**Response:** نفس الـ Settings Object مع القيم المحدثة

---

## 🐛 Common Errors

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Missing or invalid JWT token"
}
```
**الحل:** تأكد من وضع الـ JWT token الصحيح في Authorization header

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "User not found with ID: xxx"
}
```
**الحل:** تأكد من أن User ID موجود في Database

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "Validation failed"
}
```
**الحل:** تحقق من صحة البيانات المرسلة

---

## 🎯 Summary

✅ **Profile Endpoint** - يرجع بيانات المستخدم + الإحصائيات المحسوبة من DB
✅ **Settings Endpoints** - جلب وتحديث إعدادات المستخدم
✅ **Database Queries** - جميع الأرقام محسوبة ديناميكياً من Database
✅ **Authentication** - جميع الـ Endpoints محمية بـ JWT
