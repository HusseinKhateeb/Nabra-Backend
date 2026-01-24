# 🚀 Profile & Settings - Quick Start

## تم إضافة الـ Endpoints التالية:

### 1️⃣ **GET /api/v1/users/profile**
جلب ملف المستخدم الشخصي مع الإحصائيات

**Response:**
```json
{
  "id": "user-123",
  "displayName": "أحمد محمد",
  "email": "ahmad@gmail.com",
  "phoneNumber": "+972598765432",
  "age": 25,
  "avatarUrl": "...",
  "statistics": {
    "transfers": 247,      // عدد الجلسات من DB
    "hoursOfUse": 12,      // عدد المحادثات من DB
    "accuracy": 98.0       // نسبة النجاح
  },
  // ... باقي الحقول
}
```

---

### 2️⃣ **PUT /api/v1/users/profile**
تحديث الملف الشخصي

**Request:**
```json
{
  "displayName": "اسم جديد",
  "gender": "male",
  "age": 26,
  "phoneNumber": "+972...",
  "avatarUrl": "...",
  "highContrastEnabled": false,
  "fontScale": 1.2,
  "vibrationEnabled": true
}
```

---

### 3️⃣ **GET /api/v1/users/settings**
جلب إعدادات المستخدم

**Response:**
```json
{
  "userId": "user-123",
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

---

### 4️⃣ **PUT /api/v1/users/settings**
تحديث الإعدادات

**Request:**
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

---

## 🧪 اختبار سريع مع Postman

### خطوة 1: استيراد Collection
```
File → Import → اختر Nabra_Profile_Settings.postman_collection.json
```

### خطوة 2: تسجيل حساب
```
POST /api/v1/auth/register
Body:
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "TestPass123!",
  "confirmPassword": "TestPass123!",
  "displayName": "أحمد محمد"
}
```

### خطوة 3: تسجيل دخول
```
POST /api/v1/auth/login
Body:
{
  "email": "test@example.com",
  "password": "TestPass123!"
}
```
**انسخ الـ token من الـ Response**

### خطوة 4: تعيين الـ Token
في Postman → Collection Variables → ضع الـ token في `{{token}}`

### خطوة 5: اختبر الـ Endpoints
```
GET /api/v1/users/profile
GET /api/v1/users/settings
PUT /api/v1/users/settings
```

---

## 📊 ملخص البيانات المرسلة

| الـ Field | القيمة | النوع | المصدر |
|---------|--------|-------|-------|
| transfers | 247 | Long | DB (count sessions) |
| hoursOfUse | 12 | Long | DB (count chats) |
| accuracy | 98.0 | Double | User entity |
| preferredLanguage | "ar" | String | User entity |
| publicProfile | true | Boolean | User entity |
| allowNotifications | true | Boolean | User entity |

---

## 🔧 Database Changes

تم إضافة الحقول التالية في جدول users:
- `public_profile` (BOOLEAN)
- `show_contact_info` (BOOLEAN)
- `allow_notifications` (BOOLEAN)
- `allow_data_collection` (BOOLEAN)
- `data_collection_level` (VARCHAR)

---

## ✅ خصائص التطبيق

✔️ جميع الأرقام محسوبة من Database بشكل ديناميكي
✔️ جميع الـ Endpoints محمية بـ JWT
✔️ Validation كامل على جميع الـ Inputs
✔️ Response بصيغة JSON صحيحة

---

## 📝 Files Modified

1. `UserDtos.java` - إضافة Settings DTOs
2. `User.java` - إضافة Settings columns
3. `UserService.java` - إضافة Business Logic
4. `UserController.java` - إضافة الـ Endpoints
5. `SessionRepository.java` - إضافة query
6. `ChatRepository.java` - إضافة query

---

## 🎯 الخطوة التالية

الآن يمكنك:
1. ✅ بناء الـ Frontend based على هذه الـ Response
2. ✅ ربط الـ Frontend بـ Backend Endpoints
3. ✅ تعديل البيانات واختبار الـ PUT requests

جاهز للـ Frontend Development! 🚀
