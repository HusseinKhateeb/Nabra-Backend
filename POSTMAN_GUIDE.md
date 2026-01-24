# 📱 كيفية استخدام Postman Collection

## الخطوات:

### 1️⃣ افتح Postman

### 2️⃣ اضغط Import
![Import Button](https://via.placeholder.com/300x50)

### 3️⃣ اختر الملف
```
Nabra_Profile_Settings.postman_collection.json
```

### 4️⃣ تم الاستيراد ✅

الآن عندك Collection جديدة اسمها "Nabra - Profile & Settings"

---

## 📋 الـ Folders في Collection

```
Nabra - Profile & Settings
├── 🔐 Authentication
│   ├── Register
│   └── Login
├── 👤 Profile
│   ├── Get Profile
│   └── Update Profile
└── ⚙️ Settings
    ├── Get Settings
    └── Update Settings
```

---

## 🔐 خطوات الاستخدام

### خطوة 1: تسجيل حساب جديد

1. فتح **Register** folder
2. اختر **Register** request
3. عدل البيانات إذا بديت:
   ```json
   {
     "username": "ahmad",
     "email": "ahmad@example.com",
     "password": "TestPass123!",
     "confirmPassword": "TestPass123!",
     "displayName": "أحمد محمد"
   }
   ```
4. اضغط **Send**

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "550e8400...",
  "username": "ahmad",
  ...
}
```

### خطوة 2: نسخ الـ Token

1. من الـ Response انسخ قيمة `token` كاملة
2. احفظها في مكان آمن

### خطوة 3: تعيين الـ Token في Collection

1. اضغط على **Collection** في اليمين
2. اختر **Variables**
3. في السطر الـ `token`:
   - ضع القيمة المحفوظة في **INITIAL VALUE** و **CURRENT VALUE**
4. اضغط **Save**

### خطوة 4: اختبر Profile

1. فتح **👤 Profile** folder
2. اختر **Get Profile**
3. اضغط **Send**

**يجب أن تشوف:**
```json
{
  "id": "550e8400...",
  "displayName": "أحمد محمد",
  "email": "ahmad@example.com",
  "statistics": {
    "transfers": 247,
    "hoursOfUse": 12,
    "accuracy": 98.0
  },
  ...
}
```

### خطوة 5: اختبر Settings

1. فتح **⚙️ Settings** folder
2. اختر **Get Settings**
3. اضغط **Send**

**يجب أن تشوف:**
```json
{
  "userId": "550e8400...",
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

### خطوة 6: حدث Settings

1. اختر **Update Settings**
2. عدل البيانات حسب رغبتك:
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
3. اضغط **Send**

---

## 💡 نصائح مهمة

### ✅ كيفية نسخ الـ Token بسهولة

```
1. بعد ما تسجل دخول (Login Request)
2. اضغط على Response
3. شوف الـ token في JSON
4. انسخه كامل (من الـ " الأولى إلى الـ " الأخيرة)
```

### ✅ التحقق من Token صحيح

إذا طلع لك خطأ `401 Unauthorized`:
- تأكد من نسخ الـ Token صحيح
- تأكد من إدراج Token في Variables
- حاول تسجيل حساب جديد وخذ Token جديد

### ✅ إذا طلع 404 Not Found

يعني الـ User مش موجود:
- اعمل Register جديد
- اعمل Login بالـ بيانات الجديدة
- خذ الـ Token الجديد

---

## 🔄 Workflow الكامل

```
1. Register (نسجل حساب)
   ↓
2. نسخ Token من Response
   ↓
3. تعيين Token في Variables
   ↓
4. Get Profile (نشوف البيانات)
   ↓
5. Get Settings (نشوف الإعدادات)
   ↓
6. Update Settings (نحدث الإعدادات)
   ↓
7. Get Settings (تحقق من التحديث)
```

---

## 🎯 حقول Settings وشو معنى كل وحدة

| الحقل | المعنى | الخيارات |
|------|--------|----------|
| preferredLanguage | اللغة المفضلة | "ar", "en" |
| highContrastEnabled | الألوان قوية | true, false |
| fontScale | حجم الخط | 1.0, 1.2, 1.5, 2.0 |
| vibrationEnabled | الاهتزاز | true, false |
| publicProfile | الملف عام | true, false |
| showContactInfo | عرض الهاتف | true, false |
| allowNotifications | الإشعارات | true, false |
| allowDataCollection | جمع البيانات | true, false |
| dataCollectionLevel | مستوى الجمع | "basic", "standard", "full", "minimal" |

---

## 🆘 حل الأخطاء الشائعة

### ❌ Error: "Missing or invalid JWT token"
**الحل:** تأكد من إدراج Token في Authorization header بشكل صحيح

### ❌ Error: "User not found"
**الحل:** اعمل Register جديد

### ❌ Error: "Validation failed"
**الحل:** تأكد من البيانات المُرسلة صحيحة وكاملة

### ❌ Error: "Connection refused"
**الحل:** تأكد من أن Backend running على `http://localhost:8080`

---

## 📊 Test Results

| Endpoint | Expected Status | Your Result |
|----------|-----------------|-------------|
| POST /register | 201 Created | ✓ |
| POST /login | 200 OK | ✓ |
| GET /profile | 200 OK | ✓ |
| PUT /profile | 200 OK | ✓ |
| GET /settings | 200 OK | ✓ |
| PUT /settings | 200 OK | ✓ |

---

## 🎉 بعد ما تخلص الاختبار

الآن أنت جاهز للـ Frontend Development!

استخدم هذه الـ URLs والـ Response Format لبناء الـ UI الخاص بك.

---

**تم! الآن معك كل الأدوات الـ تحتاجها للـ Frontend Development.** 🚀
