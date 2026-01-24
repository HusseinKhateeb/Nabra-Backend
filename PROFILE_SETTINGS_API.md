# 🎯 Profile & Settings Endpoints Documentation

## ✅ تم إنشاء الـ Endpoints التالية:

---

## 1️⃣ Get Profile (الملف الشخصي)

### Endpoint
```
GET /api/v1/users/profile
```

### Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### Response ✅
```json
{
  "id": "user-123",
  "username": "john_doe",
  "displayName": "أحمد محمد",
  "email": "john@example.com",
  "phoneNumber": "+972598765432",
  "joinDate": "2024-01-15T10:30:00Z",
  "role": "USER",
  "status": "ACTIVE",
  "userType": "OTHER",
  "gender": "male",
  "age": 25,
  "avatarUrl": "https://example.com/avatar.jpg",
  "highContrastEnabled": false,
  "fontScale": 1.2,
  "vibrationEnabled": true,
  "emailVerified": true,
  "statistics": {
    "transfers": 247,        // ✅ محسوبة من DB (عدد الجلسات)
    "hoursOfUse": 12,        // ✅ محسوبة من DB (عدد المحادثات)
    "accuracy": 98.5         // النسبة من User entity
  }
}
```

### ملاحظات مهمة:
- `transfers`: عدد الجلسات (Sessions) المحسوبة من قاعدة البيانات
- `hoursOfUse`: عدد المحادثات (Chats) المحسوبة من قاعدة البيانات
- `accuracy`: نسبة النجاح/التقدم

---

## 2️⃣ Update Profile

### Endpoint
```
PUT /api/v1/users/profile
```

### Request Body
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

### Response
نفس format الـ Profile response أعلى

---

## 3️⃣ Get Settings (الإعدادات)

### Endpoint
```
GET /api/v1/users/settings
```

### Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### Response ✅
```json
{
  "userId": "user-123",
  "preferredLanguage": "ar",
  "highContrastEnabled": false,
  "fontScale": 1.2,
  "vibrationEnabled": true,
  "publicProfile": true,
  "showContactInfo": false,
  "allowNotifications": true,
  "allowDataCollection": true,
  "dataCollectionLevel": "basic"
}
```

### شرح الحقول:
| الحقل | المعنى |
|-----|-------|
| `preferredLanguage` | اللغة المفضلة (ar, en) |
| `highContrastEnabled` | تفعيل الألوان العالية التباين |
| `fontScale` | حجم الخط (1.0 = عادي، 1.5 = أكبر) |
| `vibrationEnabled` | تفعيل الاهتزاز عند الإشعارات |
| `publicProfile` | هل الملف عام أم خاص |
| `showContactInfo` | عرض معلومات الاتصال |
| `allowNotifications` | السماح بالإشعارات |
| `allowDataCollection` | السماح بجمع البيانات |
| `dataCollectionLevel` | مستوى جمع البيانات (basic, standard, full) |

---

## 4️⃣ Update Settings

### Endpoint
```
PUT /api/v1/users/settings
```

### Request Body
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

### Response
نفس format الـ Settings response أعلى

---

## 📊 Database Schema Changes

تم إضافة الحقول التالية في جدول `users`:

```sql
ALTER TABLE users ADD COLUMN public_profile BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN show_contact_info BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN allow_notifications BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN allow_data_collection BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN data_collection_level VARCHAR(20);
```

---

## 🔄 Query Methods Added

### SessionRepository
```java
@Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId")
long countSessionsByUserId(@Param("userId") String userId);
```

### ChatRepository
```java
@Query("SELECT COUNT(c) FROM Chat c JOIN c.participants p WHERE p.id = :userId")
long countChatsByUserId(@Param("userId") String userId);
```

---

## ✅ Files Modified/Created

### Modified:
1. `src/main/java/com/nabra/backend/modules/usermanagement/dto/UserDtos.java`
   - Added `UserSettingsResponse`
   - Added `UpdateSettingsRequest`

2. `src/main/java/com/nabra/backend/modules/usermanagement/model/User.java`
   - Added settings columns

3. `src/main/java/com/nabra/backend/modules/usermanagement/service/UserService.java`
   - Added `getProfile()` method
   - Added `getSettings()` method
   - Added `updateSettings()` method
   - Modified `toProfile()` to calculate statistics from DB

4. `src/main/java/com/nabra/backend/modules/usermanagement/controller/UserController.java`
   - Added `/profile` endpoint
   - Added `/settings` endpoints

5. `src/main/java/com/nabra/backend/modules/sessionhistory/repository/SessionRepository.java`
   - Added `countSessionsByUserId()` query

6. `src/main/java/com/nabra/backend/modules/chatcommunication/repository/ChatRepository.java`
   - Added `countChatsByUserId()` query

---

## 🧪 Testing with Postman

### خطوات الاختبار:

1. **استيراد Collection**
   - في Postman اضغط Import
   - اختر ملف `Nabra_Profile_Settings.postman_collection.json`

2. **Register تسجيل حساب جديد**
   ```
   POST /api/v1/auth/register
   ```

3. **Login تسجيل دخول**
   ```
   POST /api/v1/auth/login
   ```
   - انسخ الـ JWT token من الـ response

4. **Set Token في Postman**
   - اذهب للـ Variables في الـ Collection
   - ضع الـ token في `{{token}}`

5. **اختبر الـ Endpoints**
   - `GET /api/v1/users/profile` - جلب الملف الشخصي
   - `GET /api/v1/users/settings` - جلب الإعدادات
   - `PUT /api/v1/users/settings` - تحديث الإعدادات

---

## ⚠️ ملاحظات مهمة

✅ الأرقام في الإحصائيات محسوبة من Database بشكل ديناميكي
✅ كل المعلومات تأتي من الـ backend بشكل صحيح
✅ الـ endpoints محمية بـ JWT authentication
✅ جميع الـ validations موضوعة على الـ input

---

## 🔗 Related Endpoints

### Get Current User
```
GET /api/v1/users/me
```

### List All Users
```
GET /api/v1/users
```

### Update Current User (Old)
```
PUT /api/v1/users/me
```
