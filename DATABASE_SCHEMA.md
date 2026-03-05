# 📊 Database Schema - Profile & Settings

## تغييرات الـ Database

### جدول Users الأصلي
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    gender VARCHAR(20),
    age INT,
    avatar_url VARCHAR(400),
    preferred_language VARCHAR(10) DEFAULT 'ar',
    high_contrast_enabled BOOLEAN DEFAULT FALSE,
    font_scale DOUBLE DEFAULT 1.0,
    vibration_enabled BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_number VARCHAR(20),
    total_transfers LONG DEFAULT 0,
    hours_of_use DOUBLE DEFAULT 0.0,
    accuracy DOUBLE DEFAULT 0.0,
    last_login TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### الحقول الجديدة المضافة
```sql
ALTER TABLE users ADD COLUMN public_profile BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN show_contact_info BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN allow_notifications BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN allow_data_collection BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN data_collection_level VARCHAR(20) DEFAULT 'basic';
```

### الجدول الكامل الآن:
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    
    -- Basic Info
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    
    -- Profile Info
    gender VARCHAR(20),
    age INT,
    phone_number VARCHAR(20),
    avatar_url VARCHAR(400),
    
    -- Roles & Status
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    
    -- Accessibility Settings
    preferred_language VARCHAR(10) DEFAULT 'ar',
    high_contrast_enabled BOOLEAN DEFAULT FALSE,
    font_scale DOUBLE DEFAULT 1.0,
    vibration_enabled BOOLEAN DEFAULT TRUE,
    
    -- Privacy & Communication Settings (جديدة)
    public_profile BOOLEAN NOT NULL DEFAULT true,
    show_contact_info BOOLEAN NOT NULL DEFAULT false,
    allow_notifications BOOLEAN NOT NULL DEFAULT true,
    allow_data_collection BOOLEAN NOT NULL DEFAULT true,
    data_collection_level VARCHAR(20) DEFAULT 'basic',
    
    -- Verification & Metrics
    email_verified BOOLEAN DEFAULT FALSE,
    total_transfers LONG DEFAULT 0,
    hours_of_use DOUBLE DEFAULT 0.0,
    accuracy DOUBLE DEFAULT 0.0,
    
    -- Timestamps
    last_login TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Indexes
    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
);
```

---

## جداول ذات صلة

### جدول Sessions
```sql
CREATE TABLE sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    input_type VARCHAR(20) NOT NULL,
    output_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    duration_seconds LONG,
    recognized_text VARCHAR(2000),
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_sessions_user_started (user_id, started_at),
    INDEX idx_sessions_output_type (output_type)
);
```

### جدول Chats
```sql
CREATE TABLE chats (
    id VARCHAR(36) PRIMARY KEY,
    group_chat BOOLEAN NOT NULL DEFAULT false,
    title VARCHAR(120),
    last_message_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### جدول Chat Participants
```sql
CREATE TABLE chat_participants (
    chat_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    
    PRIMARY KEY (chat_id, user_id),
    FOREIGN KEY (chat_id) REFERENCES chats(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## Queries المستخدمة

### 1. حساب عدد الجلسات لمستخدم معين
```sql
SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId
```

**في Repository:**
```java
@Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId")
long countSessionsByUserId(@Param("userId") String userId);
```

---

### 2. حساب عدد المحادثات لمستخدم معين
```sql
SELECT COUNT(c) FROM Chat c 
JOIN c.participants p 
WHERE p.id = :userId
```

**في Repository:**
```java
@Query("SELECT COUNT(c) FROM Chat c JOIN c.participants p WHERE p.id = :userId")
long countChatsByUserId(@Param("userId") String userId);
```

---

## Java Entity المحدث

### User.java
```java
@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {
    
    // ... الحقول الموجودة ...
    
    // 🆕 Settings Fields
    @Column(nullable = false)
    private boolean publicProfile = true;
    
    @Column(nullable = false)
    private boolean showContactInfo = false;
    
    @Column(nullable = false)
    private boolean allowNotifications = true;
    
    @Column(nullable = false)
    private boolean allowDataCollection = true;
    
    @Column(length = 20)
    private String dataCollectionLevel = "basic";
}
```

---

## Migration Script (للـ Flyway/Liquibase)

### V5__Add_Settings_Columns.sql
```sql
-- Add Settings Columns to Users Table
ALTER TABLE users 
ADD COLUMN public_profile BOOLEAN NOT NULL DEFAULT true AFTER allow_data_collection,
ADD COLUMN show_contact_info BOOLEAN NOT NULL DEFAULT false AFTER public_profile,
ADD COLUMN allow_notifications BOOLEAN NOT NULL DEFAULT true AFTER show_contact_info,
ADD COLUMN allow_data_collection BOOLEAN NOT NULL DEFAULT true AFTER allow_notifications,
ADD COLUMN data_collection_level VARCHAR(20) DEFAULT 'basic' AFTER allow_data_collection;
```

---

## Data Types Mapping

| SQL Type | Java Type | Purpose |
|----------|-----------|---------|
| VARCHAR(36) | String | UUIDs |
| VARCHAR(50) | String | Username |
| VARCHAR(120) | String | Email |
| VARCHAR(80) | String | Display Name |
| VARCHAR(20) | String | Enums (role, status) |
| INT | Integer | Age |
| LONG | long | Counters |
| DOUBLE | double | Percentages |
| BOOLEAN | boolean | Flags |
| VARCHAR(400) | String | URLs |
| TIMESTAMP | Instant | Dates |

---

## جداول الـ Blocking (User Blocking)

```sql
CREATE TABLE user_blocks (
    blocker_id VARCHAR(36) NOT NULL,
    blocked_id VARCHAR(36) NOT NULL,
    
    PRIMARY KEY (blocker_id, blocked_id),
    FOREIGN KEY (blocker_id) REFERENCES users(id),
    FOREIGN KEY (blocked_id) REFERENCES users(id),
    INDEX idx_blocked_id (blocked_id)
);
```

---

## نموذج البيانات الكامل

```
Users (الجدول الرئيسي)
├── Basic Info (username, email, etc.)
├── Profile Info (age, gender, avatar)
├── Settings (language, contrast, etc.)
├── Privacy Settings (🆕 public_profile, etc.)
├── Metrics (accuracy, total_transfers)
└── Relations
    └── Sessions (1:Many)
    └── Chats (Many:Many via chat_participants)
    └── Blocked Users (Many:Many)
```

---

## Indexes للـ Performance

```sql
-- Existing
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- For Sessions queries
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_user_started ON sessions(user_id, started_at);

-- For Chat queries
CREATE INDEX idx_chat_participants_user_id ON chat_participants(user_id);
CREATE INDEX idx_chat_participants_chat_id ON chat_participants(chat_id);
```

---

## ملخص التغييرات

| الحقل | النوع | القيمة الافتراضية | الغرض |
|------|------|-----------------|-------|
| public_profile | BOOLEAN | true | هل الملف عام |
| show_contact_info | BOOLEAN | false | عرض معلومات الاتصال |
| allow_notifications | BOOLEAN | true | السماح بالإشعارات |
| allow_data_collection | BOOLEAN | true | جمع البيانات |
| data_collection_level | VARCHAR | "basic" | مستوى جمع البيانات |

---

## ملاحظات مهمة

✅ تم استخدام Hibernate's `@Column` annotations
✅ جميع الحقول الجديدة لها قيم افتراضية آمنة
✅ تم إضافة Indexes للـ Performance
✅ Migration مع Spring Data JPA automatic ddl-auto

🔧 إذا كنت تستخدم موقع staging/production مهم أن تعمل migration يدوية بحذر!
