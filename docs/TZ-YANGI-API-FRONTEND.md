# PHARM — Yangi API va Frontend Texnik Topshiriq (TZ)

> **Versiya:** 1.0  
> **Sana:** 2026-09-02  
> **Maqsad:** Mavjud ishlayotgan tizimni (5000+ buyurtma, faol mijozlar bazasi) buzmasdan backend va frontendni ajratib, zamonaviy UI/UX bilan qayta qurish.  
> **Asosiy qoida:** Database strukturasi va entity modellari **O'ZGARMAYDI**. Yangi loyiha mavjud PostgreSQL bazasiga ulanadi.

---

## 1. Loyiha haqida umumiy ma'lumot

### 1.1. Tizim tavsifi

**PHARM** — dorixona/distribution buyurtmalarini boshqarish tizimi. Admin ombor va katalogni boshqaradi, buyurtma yaratadi va kuryerga tayinlaydi. Kuryer buyurtmalarni yetkazadi, o'z zaxirasini va naqd pulini kuzatadi. Viewer faqat ko'rish huquqiga ega.

### 1.2. Hozirgi texnologiyalar (eski loyiha)

| Qatlam | Texnologiya |
|--------|-------------|
| Backend | Spring Boot 4.0.3, Java 21, Spring MVC + REST |
| Database | PostgreSQL |
| Cache | Redis (JWT blacklist) |
| Auth | Spring Security (form login + JWT) |
| Frontend | Thymeleaf + inline Vue 3 + Bootstrap 4 + axios |
| Mobile API | `/api/mobile/**` (JWT) |

### 1.3. Hozirgi muammolar

- Backend va frontend bitta Spring Boot jar ichida aralashgan
- Thymeleaf + inline Vue — zamonaviy SPA emas
- API standartlashtirilmagan (REST + form POST aralash)
- OPERATOR roli to'liq implement qilinmagan
- Flyway o'chirilgan, schema Hibernate `ddl-auto: update` bilan boshqariladi
- JWT secret hardcoded
- Global exception handler o'chirilgan

### 1.4. Yangi loyiha maqsadi

```
pharm/                    ← eski loyiha (ishlayveradi, o'zgartirilmaydi)
pharm-v2/                 ← YANGI loyiha (0 dan quriladi)
├── pharm-api/            ← Spring Boot REST API (faqat JSON)
└── pharm-web/            ← Vue 3 SPA (yoki React — tanlov)
```

---

## 2. Muhim cheklovlar va qoidalar

### 2.1. Database — O'ZGARMAYDI

Yangi loyiha **mavjud production/test bazasiga** ulanadi. Quyidagi jadval nomlari va strukturalari saqlanishi **SHART**:

| Jadval | Entity | Izoh |
|--------|--------|------|
| `_users` | User | Foydalanuvchilar |
| `customers` | Customer | Mijozlar |
| `addresses` | Address | Mijoz manzillari |
| `categories` | Category | Kategoriyalar |
| `sub_categories` | SubCategory | Subkategoriyalar |
| `products` | Product | Mahsulotlar |
| `store` | Store | Ombor zaxirasi |
| `store_histories` | StoreHistory | Ombor kirim tarixi |
| `orders` | Order | Buyurtmalar |
| `order_items` | OrderItem | Buyurtma pozitsiyalari |
| `order_delivered_info` | OrderDeliveredInfo | Yetkazish izohlari/rasmlar |
| `courier_stocks` | CourierStock | Kuryer zaxirasi |
| `courier_stock_histories` | CourierStockHistory | Kuryer zaxira tarixi |
| `cash_registers` | CashRegister | Kuryer naqd puli |
| `sale_logs` | SaleLog | Sotuv statistikasi |

> **DIQQAT:** `ddl-auto` yangi loyihada **`validate`** yoki **`none`** bo'lishi kerak. **`update`/`create` ISHLATILMAYDI** — mavjud ma'lumotlar buzilmasligi uchun.

### 2.2. Entity modellari

Barcha 15 ta entity klassi (`uz.uwon.pharm.*`) yangi API loyihasiga **1:1 nusxa** qilinadi:
- Field nomlari
- `@Table` nomlari
- Enum qiymatlari (STRING/ORDINAL — hozirgi holatda saqlanadi)
- Relationship mapping (LAZY/EAGER)
- `@PrePersist` lifecycle hooklar

### 2.3. Mavjud ma'lumotlar

- 5000+ buyurtma
- Faol mijozlar bazasi
- Mahsulotlar, kategoriyalar, ombor zaxiralari
- Kuryer zaxiralari va naqd pul tarixi
- Sotuv loglari

Yangi tizim ishga tushganda barcha ma'lumotlar **darhol ko'rinishi** kerak.

---

## 3. Foydalanuvchi rollari va huquqlari

### 3.1. Rollar

| Rol | Tavsif | Hozirgi holat |
|-----|--------|---------------|
| **ADMIN** | To'liq boshqaruv: foydalanuvchilar, katalog, ombor, buyurtmalar, kuryer operatsiyalari, hisobotlar | To'liq ishlaydi |
| **OPERATOR** | Buyurtma qabul qilish va boshqarish (admin panelning cheklangan qismi) | **To'liq implement qilinmagan** — yangi loyihada aniqlash kerak |
| **COURIER** | Buyurtmalarni ko'rish, yetkazish, status o'zgartirish, zaxira va naqd pul | To'liq ishlaydi |
| **VIEWER** | Faqat ko'rish: dashboard, buyurtmalar, statistika (DELETED buyurtmalar ko'rinmaydi) | To'liq ishlaydi |

### 3.2. Yangi loyihada OPERATOR roli (taklif)

OPERATOR quyidagi huquqlarga ega bo'lsin:

| Modul | Huquq |
|-------|-------|
| Mijozlar | CRUD (yaratish, ko'rish, tahrirlash) |
| Buyurtmalar | Yaratish, ko'rish, status o'zgartirish (DELIVERED/CANCELLED/DELETED dan tashqari) |
| Mahsulotlar | Faqat ko'rish |
| Katalog | Faqat ko'rish |
| Foydalanuvchilar | Yo'q |
| Ombor | Yo'q |
| Kuryer zaxirasi | Yo'q |
| Naqd pul | Yo'q |
| Hisobotlar | Faqat ko'rish (dashboard) |

> **Qaror kerak:** OPERATOR rolini shu tarzda implement qilish yoki ADMIN dan farqlanmaydigan qilish — loyiha boshlanishidan oldin tasdiqlansin.

### 3.3. Rol-based API access matrix

| Endpoint guruhi | ADMIN | OPERATOR | COURIER | VIEWER |
|-----------------|-------|----------|---------|--------|
| `/api/v1/auth/**` | ✅ | ✅ | ✅ | ✅ |
| `/api/v1/users/**` | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/customers/**` | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/addresses/**` | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/categories/**` | ✅ CRUD | ✅ Read | ❌ | ❌ |
| `/api/v1/products/**` | ✅ CRUD | ✅ Read | ❌ | ❌ |
| `/api/v1/store/**` | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/orders/**` | ✅ | ✅ (cheklangan) | ✅ (o'z buyurtmalari) | ✅ Read |
| `/api/v1/courier-stock/**` | ✅ | ❌ | ✅ (o'zi) | ❌ |
| `/api/v1/cash-register/**` | ✅ | ❌ | ✅ (o'zi) | ❌ |
| `/api/v1/statistics/**` | ✅ | ✅ Read | ✅ (o'zi) | ✅ Read |
| `/api/v1/sale-logs/**` | ✅ | ✅ Read | ❌ | ✅ Read |

---

## 4. Database entity modellari (to'liq spetsifikatsiya)

### 4.1. User (`_users`)

```
id              BIGINT PK AUTO
full_name       VARCHAR
username        VARCHAR UNIQUE NOT NULL
password        VARCHAR (BCrypt)
personal_phone  VARCHAR
work_phone      VARCHAR
status          ENUM: ACTIVE | BLOCKED | DELETED
role            ENUM: ADMIN | OPERATOR | COURIER | VIEWER
created_at      DATE
updated_at      DATE
```

**Relationships:**
- OneToMany → Order (courier sifatida)
- OneToMany → CashRegister

### 4.2. Customer (`customers`)

```
id          BIGINT PK AUTO
full_name   VARCHAR
phone       VARCHAR UNIQUE
birth_date  DATE
description VARCHAR
gender      ENUM: MALE | FEMALE | NOT_SELECTED (ORDINAL!)
type        ENUM: B2C | B2B (ORDINAL!)
status      ENUM: ACTIVE | BLOCKED | DELETED
created_at  DATE
updated_at  DATE
```

**Relationships:**
- OneToMany → Address
- OneToMany → Order

**Business rules:**
- `@PrePersist`: default status=ACTIVE, gender=NOT_SELECTED
- Soft delete: status=DELETED
- Telefon raqami unique

### 4.3. Address (`addresses`)

```
id           BIGINT PK AUTO
full_address VARCHAR
home         VARCHAR
entrance     VARCHAR
apartment    VARCHAR
floor        VARCHAR
orientation  VARCHAR
latitude     DECIMAL
longitude    DECIMAL
status       ENUM: ACTIVE | BLOCKED | DELETED
created_at   DATE
updated_at   DATE
customer_id  FK → customers
```

### 4.4. Category (`categories`)

```
id          BIGINT PK AUTO
name        VARCHAR
description VARCHAR
status      ENUM (STRING)
created_at  DATE
updated_at  DATE
```

**Relationships:** OneToMany → SubCategory, Product

### 4.5. SubCategory (`sub_categories`)

```
id          BIGINT PK AUTO
name        VARCHAR
description VARCHAR
status      ENUM (STRING)
created_at  DATE
updated_at  DATE
category_id FK → categories (LAZY)
```

### 4.6. Product (`products`)

```
id              BIGINT PK AUTO
name            VARCHAR
price           DECIMAL (sotuv narxi)
price_cost      DECIMAL (tannarx)
sort_number     INTEGER
status          ENUM (STRING)
description     VARCHAR
photo           VARCHAR (fayl nomi)
unit_type       ENUM: PCS | BOX | KG (STRING)
created_at      DATE
updated_at      DATE
category_id     FK → categories (EAGER)
sub_category_id FK → sub_categories (LAZY)
```

**Relationships:**
- OneToOne → Store
- OneToMany → OrderItem

### 4.7. Store (`store`) — Ombor

```
id              BIGINT PK AUTO
product_id      FK → products (OneToOne)
quantity        DECIMAL
total_amount    DECIMAL
date_of_arrival DATE
status          ENUM (STRING)
created_at      DATETIME
updated_at      DATETIME
```

### 4.8. StoreHistory (`store_histories`)

```
id              BIGINT PK AUTO
product_name    VARCHAR
quantity        DECIMAL
price_cost      DECIMAL
total_amount    DECIMAL
date_of_arrival DATE
comment         VARCHAR
status          ENUM (STRING)
store_id        BIGINT (FK emas, oddiy Long!)
created_at      DATETIME
updated_at      DATETIME
```

### 4.9. Order (`orders`)

```
id              BIGINT PK AUTO
order_number    VARCHAR UNIQUE (auto: "ORD - XXXX")
customer_id     FK → customers (LAZY)
user_id         FK → _users (courier, LAZY)
order_status    ENUM (STRING): NEW | CONFIRMED | PROCESSING | SHIPPED | PENDING | DELIVERED | CANCELLED | DELETED
total_sum       DECIMAL
address         VARCHAR
home            VARCHAR
entrance        VARCHAR
apartment       VARCHAR
floor           VARCHAR
orientations    VARCHAR
payment_type    ENUM: CASH | CARD (ORDINAL!)
created_at      DATETIME
updated_at      DATETIME
```

**Relationships:**
- OneToMany → OrderItem
- OneToMany → OrderDeliveredInfo

**Business rules:**
- `@PrePersist`: orderNumber auto-generate, default status=NEW
- Hozirgi `save()` metodida yangi buyurtma to'g'ridan-to'g'ri SHIPPED holatda yaratiladi
- VIEWER roli DELETED buyurtmalarni ko'rmaydi

### 4.10. OrderItem (`order_items`)

```
id          BIGINT PK AUTO
order_id    FK → orders (LAZY)
product_id  FK → products (EAGER)
quantity    DECIMAL
price       DECIMAL
total_sum   DECIMAL
is_bonus    BOOLEAN (default false)
created_at  DATE
updated_at  DATE
```

### 4.11. OrderDeliveredInfo (`order_delivered_info`)

```
id          BIGINT PK AUTO
order_id    FK → orders (LAZY)
comment     VARCHAR
image       VARCHAR (fayl nomi)
created_at  DATETIME
updated_at  DATETIME
```

### 4.12. CourierStock (`courier_stocks`) — Denormalized

```
id              BIGINT PK AUTO
courier_id      BIGINT (FK emas!)
product_id      BIGINT (FK emas!)
product_name    VARCHAR
quantity        DECIMAL
product_price   DECIMAL
total_amount    DECIMAL
unit_type       ENUM (STRING)
created_at      DATETIME
update_at       DATETIME
```

### 4.13. CourierStockHistory (`courier_stock_histories`) — Denormalized

```
id               BIGINT PK AUTO
courier_id       BIGINT
product_id       BIGINT
product_name     VARCHAR
before_quantity  DECIMAL
change_quantity  DECIMAL
after_quantity   DECIMAL
product_price    DECIMAL
total_amount     DECIMAL
unit_type        ENUM (STRING)
action_type      ENUM: INCOME | OUTCOME | SALE | RETURN (STRING)
comment          VARCHAR
created_at       DATETIME
```

### 4.14. CashRegister (`cash_registers`)

```
id               BIGINT PK AUTO
courier_id       FK → _users (LAZY)
order_id         BIGINT
order_total_sum  DECIMAL
register_status  ENUM: ON_COURIER | ON_ADMIN (STRING)
date_time        DATETIME
comment          VARCHAR
status           ENUM (STRING)
created_at       DATETIME
update_at        DATETIME
```

### 4.15. SaleLog (`sale_logs`) — Denormalized audit

```
id                BIGINT PK AUTO
order_id          BIGINT
category_id       BIGINT
category_name     VARCHAR
customer_name     VARCHAR
customer_phone    VARCHAR
customer_address  VARCHAR
product_name      VARCHAR
price             DECIMAL
product_price_cost DECIMAL
quantity          DECIMAL
total_sum         DECIMAL
is_bonus          BOOLEAN
created_at        DATETIME
updated_at        DATETIME
```

---

## 5. Biznes jarayonlari (Business Logic)

### 5.1. Buyurtma hayot sikli

```
NEW → CONFIRMED → PROCESSING → SHIPPED → PENDING → DELIVERED
                                              ↓
                                          CANCELLED
                                              ↓
                                          DELETED
```

**Status o'zgarish qoidalari:**

| Yangi status | Amal |
|-------------|------|
| CANCELLED / DELETED | Kuryer zaxirasiga mahsulotlar qaytariladi |
| DELIVERED | CashRegister yaratiladi + SaleLog yozuvlari yaratiladi |
| DELIVERED / CANCELLED / DELETED | Keyingi o'zgarishlar **bloklanadi** (final status) |

**Buyurtma yaratish (hozirgi mantiq):**
1. Mijoz tanlash yoki yangi yaratish (telefon bo'yicha qidirish)
2. Kuryer tanlash
3. Kuryer zaxirasidan mahsulot tanlash (stock validation!)
4. Bonus mahsulotlar (totalSum=0, isBonus=true)
5. Buyurtma SHIPPED holatda yaratiladi
6. Kuryer zaxirasi kamayadi

### 5.2. Ombor → Kuryer zaxirasi transferi

```
Admin: Ombor (store) → Kuryer zaxirasi (courier_stocks)
1. Omborda yetarli miqdor tekshiriladi
2. Ombor miqdori kamayadi
3. Kuryer zaxirasiga qo'shiladi (yoki yangi yozuv yaratiladi)
4. CourierStockHistory: actionType=INCOME
```

### 5.3. Kuryer zaxirasini qaytarish

```
Admin: Kuryer zaxirasi → Ombor
1. Kuryer zaxirasidan miqdor olib tashlanadi
2. Omborga qaytariladi
3. CourierStockHistory: actionType=RETURN
```

### 5.4. Naqd pul jarayoni

```
Buyurtma DELIVERED → CashRegister yaratiladi (ON_COURIER)
Admin naqd pulni qabul qiladi → registerStatus=ON_ADMIN
```

### 5.5. Soft delete qoidalari

| Entity | Usul |
|--------|------|
| User | status=DELETED |
| Customer | status=DELETED (restore mumkin) |
| Order | orderStatus=DELETED |
| Category/Product | status=DELETED |

---

## 6. Yangi Backend API (pharm-api)

### 6.1. Texnologiya stack

| Komponent | Tanlov |
|-----------|--------|
| Framework | Spring Boot 4.x, Java 21 |
| API | REST JSON (faqat `@RestController`, Thymeleaf yo'q) |
| Auth | JWT (access + refresh token) |
| Docs | OpenAPI 3 / Swagger UI |
| Validation | Jakarta Validation |
| Mapping | MapStruct (tavsiya) yoki manual |
| DB | PostgreSQL, JPA/Hibernate, `ddl-auto: validate` |
| Cache | Redis |
| File storage | Local filesystem (`uploads/`) |
| CORS | Frontend domeni uchun sozlangan |

### 6.2. API versiyalash

Barcha endpointlar `/api/v1/` prefiksi ostida:

```
/api/v1/auth/login          POST
/api/v1/auth/refresh        POST
/api/v1/auth/logout         POST
/api/v1/auth/me             GET
```

### 6.3. To'liq API endpointlar ro'yxati

#### 6.3.1. Auth

| Method | Endpoint | Tavsif | Auth |
|--------|----------|--------|------|
| POST | `/api/v1/auth/login` | Login (username + password → JWT) | Public |
| POST | `/api/v1/auth/refresh` | Refresh token | Public |
| POST | `/api/v1/auth/logout` | Logout (JWT blacklist) | Auth |
| GET | `/api/v1/auth/me` | Joriy foydalanuvchi ma'lumoti | Auth |

#### 6.3.2. Users (ADMIN only)

| Method | Endpoint | Tavsif |
|--------|----------|--------|
| GET | `/api/v1/users` | Ro'yxat (pagination, filter by role/status) |
| GET | `/api/v1/users/{id}` | Bitta foydalanuvchi |
| POST | `/api/v1/users` | Yaratish |
| PUT | `/api/v1/users/{id}` | Yangilash |
| PATCH | `/api/v1/users/{id}/block` | Bloklash |
| PATCH | `/api/v1/users/{id}/unblock` | Blokdan chiqarish |
| DELETE | `/api/v1/users/{id}` | Soft delete |
| GET | `/api/v1/users/couriers` | Faqat kuryerlar ro'yxati |
| GET | `/api/v1/users/check-username?username=` | Username mavjudligini tekshirish |
| GET | `/api/v1/users/statistics` | Foydalanuvchilar statistikasi |

#### 6.3.3. Customers

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/customers` | Ro'yxat (pagination, search) | ADMIN, OPERATOR |
| GET | `/api/v1/customers/{id}` | Bitta mijoz | ADMIN, OPERATOR |
| GET | `/api/v1/customers/search?phone=` | Telefon bo'yicha qidirish | ADMIN, OPERATOR |
| POST | `/api/v1/customers` | Yaratish | ADMIN, OPERATOR |
| PUT | `/api/v1/customers/{id}` | Yangilash | ADMIN, OPERATOR |
| DELETE | `/api/v1/customers/{id}` | Soft delete | ADMIN |
| PATCH | `/api/v1/customers/{id}/restore` | Tiklash | ADMIN |
| GET | `/api/v1/customers/check-phone?phone=` | Telefon tekshirish | ADMIN, OPERATOR |

#### 6.3.4. Addresses

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/customers/{customerId}/addresses` | Mijoz manzillari | ADMIN, OPERATOR |
| GET | `/api/v1/addresses/{id}` | Bitta manzil | ADMIN, OPERATOR |
| POST | `/api/v1/customers/{customerId}/addresses` | Manzil qo'shish | ADMIN, OPERATOR |
| PUT | `/api/v1/addresses/{id}` | Yangilash | ADMIN, OPERATOR |
| DELETE | `/api/v1/addresses/{id}` | Soft delete | ADMIN |

#### 6.3.5. Categories & SubCategories

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/categories` | Barcha kategoriyalar | ADMIN, OPERATOR |
| GET | `/api/v1/categories/{id}` | Bitta kategoriya | ADMIN, OPERATOR |
| POST | `/api/v1/categories` | Yaratish | ADMIN |
| PUT | `/api/v1/categories/{id}` | Yangilash | ADMIN |
| DELETE | `/api/v1/categories/{id}` | Soft delete | ADMIN |
| GET | `/api/v1/categories/{id}/subcategories` | Subkategoriyalar | ADMIN, OPERATOR |
| POST | `/api/v1/categories/{id}/subcategories` | Subkategoriya yaratish | ADMIN |
| PUT | `/api/v1/subcategories/{id}` | Yangilash | ADMIN |
| DELETE | `/api/v1/subcategories/{id}` | Soft delete | ADMIN |

#### 6.3.6. Products

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/products` | Ro'yxat (pagination, filter, search) | ADMIN, OPERATOR |
| GET | `/api/v1/products/{id}` | Bitta mahsulot | ADMIN, OPERATOR |
| GET | `/api/v1/products/in-stock` | Zaxirasi bor mahsulotlar | ADMIN, OPERATOR |
| POST | `/api/v1/products` | Yaratish (+ rasm upload) | ADMIN |
| PUT | `/api/v1/products/{id}` | Yangilash | ADMIN |
| DELETE | `/api/v1/products/{id}` | Soft delete | ADMIN |
| GET | `/api/v1/products/{id}/image` | Mahsulot rasmi | Auth |
| GET | `/api/v1/products/search?q=` | Qidirish | ADMIN, OPERATOR |

#### 6.3.7. Store (Ombor)

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/store` | Ombor ro'yxati | ADMIN |
| GET | `/api/v1/store/{id}` | Bitta yozuv | ADMIN |
| POST | `/api/v1/store/arrival` | Omborga kirim | ADMIN |
| GET | `/api/v1/store/history` | Kirim tarixi | ADMIN |
| GET | `/api/v1/store/statistics` | Ombor statistikasi | ADMIN |

#### 6.3.8. Orders

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/orders` | Ro'yxat (pagination, filter: status, phone, courierId, dateRange) | ADMIN, OPERATOR, VIEWER |
| GET | `/api/v1/orders/{id}` | Buyurtma detali | ADMIN, OPERATOR, COURIER*, VIEWER |
| POST | `/api/v1/orders` | Buyurtma yaratish | ADMIN, OPERATOR |
| PUT | `/api/v1/orders/{id}` | Buyurtma tahrirlash | ADMIN, COURIER* |
| PATCH | `/api/v1/orders/{id}/status` | Status o'zgartirish | ADMIN, OPERATOR, COURIER* |
| GET | `/api/v1/orders/{id}/comments` | Yetkazish izohlari | ADMIN, OPERATOR, COURIER*, VIEWER |
| POST | `/api/v1/orders/{id}/comments` | Izoh qo'shish (+ rasm) | ADMIN, COURIER* |
| GET | `/api/v1/orders/comments/images/{filename}` | Izoh rasmi | Auth |

> *COURIER faqat o'z buyurtmalariga

#### 6.3.9. Courier Stock

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/courier-stock/{courierId}` | Kuryer zaxirasi | ADMIN, COURIER* |
| GET | `/api/v1/courier-stock/{courierId}/history` | Zaxira tarixi | ADMIN |
| POST | `/api/v1/courier-stock/transfer` | Ombordan kuryerga transfer | ADMIN |
| POST | `/api/v1/courier-stock/return` | Kuryerdan omborga qaytarish | ADMIN |
| GET | `/api/v1/courier-stock/products/search?q=` | Kuryer mahsulotlari qidirish | ADMIN, OPERATOR |

#### 6.3.10. Cash Register

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/cash-register/courier/{courierId}` | Kuryer naqd puli | ADMIN, COURIER* |
| GET | `/api/v1/cash-register/courier/{courierId}/total` | Jami naqd pul | ADMIN, COURIER* |
| PATCH | `/api/v1/cash-register/{id}/return` | Naqd pulni qaytarish (admin qabul qiladi) | ADMIN |
| GET | `/api/v1/cash-register/daily-report?date=` | Kunlik hisobot | ADMIN |

#### 6.3.11. Statistics

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/statistics/dashboard` | Dashboard statistikasi | ADMIN, OPERATOR, VIEWER |
| GET | `/api/v1/statistics/daily?from=&to=` | Kunlik statistika | ADMIN, OPERATOR, VIEWER |
| GET | `/api/v1/statistics/courier/{courierId}` | Kuryer statistikasi | ADMIN, COURIER* |
| GET | `/api/v1/statistics/courier-monthly?start=&end=` | Kuryerlar oylik reytingi | ADMIN |
| GET | `/api/v1/statistics/courier/{courierId}/delivered` | Yetkazilgan buyurtmalar | COURIER*, ADMIN |
| GET | `/api/v1/statistics/courier/{courierId}/cancelled` | Bekor qilingan | COURIER*, ADMIN |
| GET | `/api/v1/statistics/courier/{courierId}/pending` | Kutilayotgan | COURIER*, ADMIN |

#### 6.3.12. Sale Logs

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/sale-logs` | Sotuv loglari (filter: dateRange, categoryId) | ADMIN, VIEWER |

#### 6.3.13. Courier-specific (mobil/kuryer panel)

| Method | Endpoint | Tavsif | Rollar |
|--------|----------|--------|--------|
| GET | `/api/v1/courier/dashboard` | Kuryer dashboard | COURIER |
| GET | `/api/v1/courier/orders` | Mening buyurtmalarim (SHIPPED) | COURIER |
| GET | `/api/v1/courier/orders/{id}` | Buyurtma detali | COURIER |
| PATCH | `/api/v1/courier/orders/{id}/status` | Status yangilash (+ comment/image) | COURIER |
| GET | `/api/v1/courier/pending-count` | Kutilayotgan buyurtmalar soni | COURIER |
| GET | `/api/v1/courier/stock` | Mening zaxiram | COURIER |
| GET | `/api/v1/courier/cash` | Mening naqd pulim | COURIER |

### 6.4. API Response format (standart)

**Muvaffaqiyatli javob:**
```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 5234,
    "totalPages": 262
  }
}
```

**Xato javob:**
```json
{
  "success": false,
  "error": {
    "code": "STOCK_NOT_ENOUGH",
    "message": "Mahsulot yetarli emas. Product: Aspirin, mavjud: 5, kerak: 10",
    "timestamp": "2026-09-02T14:00:00"
  }
}
```

**Pagination parametrlari (barcha list endpointlar):**
```
?page=0&size=20&sort=createdAt,desc
```

### 6.5. Autentifikatsiya

| Parametr | Qiymat |
|----------|--------|
| Access token | JWT, 24 soat |
| Refresh token | JWT, 7 kun |
| Algorithm | HS256 |
| Secret | Environment variable (`JWT_SECRET`) |
| Blacklist | Redis |
| Header | `Authorization: Bearer <token>` |

### 6.6. File upload

| Parametr | Qiymat |
|----------|--------|
| Max file size | 30MB |
| Product images | `uploads/products/` |
| Delivery comment images | `uploads/comments/` |
| Format | multipart/form-data |

### 6.7. Backend loyiha strukturasi

```
pharm-api/
├── pom.xml
├── src/main/java/uz/uwon/pharm/
│   ├── PharmApiApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   ├── RedisConfig.java
│   │   └── OpenApiConfig.java
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── AuthService.java
│   │   ├── JwtService.java
│   │   ├── JwtFilter.java
│   │   └── TokenBlacklist.java
│   ├── common/
│   │   ├── ApiResponse.java
│   │   ├── PageResponse.java
│   │   ├── ErrorResponse.java
│   │   └── GlobalExceptionHandler.java
│   ├── users/          (entity, repo, service, controller, dto)
│   ├── customer/
│   ├── address/
│   ├── category/
│   ├── subcategory/
│   ├── product/
│   ├── store/
│   ├── storehistory/
│   ├── order/
│   ├── orderitem/
│   ├── orderdeliveredinfo/
│   ├── courierstock/
│   ├── courierstockhistory/
│   ├── cashregister/
│   ├── salelog/
│   ├── statistics/
│   ├── storage/
│   └── utils/
└── src/main/resources/
    ├── application.yaml
    ├── application-dev.yml
    └── application-prod.yml
```

---

## 7. Yangi Frontend (pharm-web)

### 7.1. Texnologiya stack (tavsiya)

| Komponent | Tanlov | Sabab |
|-----------|--------|-------|
| Framework | **Vue 3** + Composition API | Jamoa Vue bilan tanish |
| Build | **Vite** | Tez dev server va build |
| UI Kit | **PrimeVue** yoki **Vuetify 3** yoki **Naive UI** | Zamonaviy, responsive |
| State | **Pinia** | Vue 3 rasmiy store |
| Router | **Vue Router 4** | SPA routing |
| HTTP | **Axios** | Interceptor (JWT refresh) |
| Charts | **Chart.js** yoki **ApexCharts** | Dashboard grafiklar |
| Maps | **Yandex Maps API** | Manzil xaritasi (mavjud) |
| Icons | **PrimeIcons** / **Material Icons** | - |
| i18n | **Vue I18n** | O'zbek/Rus til qo'llab-quvvatlash |
| CSS | **Tailwind CSS** yoki UI kit built-in | Responsive design |

### 7.2. Dizayn tamoyillari

- **Mobile-first** — kuryer paneli mobil qurilmalarda ishlaydi
- **Dark/Light mode** — ixtiyoriy
- **Responsive** — desktop (admin), tablet, mobile (courier)
- **O'zbek/Rus til** — hozirgi tizim rus tilida, yangi UI ikkala til
- **Accessibility** — WCAG 2.1 AA darajasi

### 7.3. Sahifalar va routing

#### 7.3.1. Umumiy

| Route | Sahifa | Auth |
|-------|--------|------|
| `/login` | Login sahifasi | Public |
| `/` | Role-based redirect | Auth |

#### 7.3.2. ADMIN panel (`/admin/*`)

| Route | Sahifa | Hozirgi ekvivalenti |
|-------|--------|---------------------|
| `/admin/dashboard` | Dashboard (statistika, grafiklar) | `admin/dashboard.html` |
| `/admin/users` | Foydalanuvchilar CRUD | `admin/users.html` |
| `/admin/customers` | Mijozlar CRUD | `admin/customers.html` |
| `/admin/customers/:id/addresses` | Mijoz manzillari + xarita | `admin/address.html` |
| `/admin/categories` | Kategoriyalar CRUD | `admin/categories.html` |
| `/admin/categories/:id/subcategories` | Subkategoriyalar | `admin/subcategory.html` |
| `/admin/products` | Mahsulotlar CRUD | `admin/products.html` |
| `/admin/store` | Ombor boshqaruvi | `admin/store.html` |
| `/admin/store/history` | Ombor kirim tarixi | `admin/storehistory.html` |
| `/admin/orders` | Buyurtmalar ro'yxati | `admin/orders.html` |
| `/admin/orders/create` | Buyurtma yaratish | `admin/order/create.html` |
| `/admin/orders/:id` | Buyurtma detali | (modal → sahifa) |
| `/admin/courier-stock` | Kuryer zaxirasi transfer | `admin/courierstock.html` |
| `/admin/courier-stock/history` | Zaxira tarixi | `admin/courierStockHistory.html` |
| `/admin/courier-statistics` | Kuryerlar statistikasi | `admin/courierStatistic.html` |
| `/admin/cash-register` | Naqd pul boshqaruvi | `admin/cashRegister.html` |
| `/admin/cash-report` | Kunlik naqd pul hisoboti | `admin/cashStat.html` |
| `/admin/sale-statistics` | Sotuv statistikasi | `admin/saleLog.html` |

#### 7.3.3. OPERATOR panel (`/operator/*`)

| Route | Sahifa |
|-------|--------|
| `/operator/dashboard` | Dashboard (read-only statistika) |
| `/operator/customers` | Mijozlar CRUD |
| `/operator/orders` | Buyurtmalar (yaratish + boshqarish) |
| `/operator/orders/create` | Buyurtma yaratish |

#### 7.3.4. COURIER panel (`/courier/*`)

| Route | Sahifa | Hozirgi ekvivalenti |
|-------|--------|---------------------|
| `/courier/dashboard` | Dashboard | `courier/dashboard.html` |
| `/courier/orders` | Mening buyurtmalarim | `courier/orders.html` |
| `/courier/orders/:id` | Buyurtma detali/tahrirlash | `courier/orderUpdate.html` |
| `/courier/statistics` | Yetkazish statistikasi | `courier/stat.html` |
| `/courier/stock` | Mening zaxiram | `courier/product.html` |
| `/courier/cash` | Mening naqd pulim | `courier/cash.html` |

#### 7.3.5. VIEWER panel (`/viewer/*`)

| Route | Sahifa |
|-------|--------|
| `/viewer/dashboard` | Dashboard (read-only) |
| `/viewer/orders` | Buyurtmalar (read-only) |
| `/viewer/statistics` | Sotuv statistikasi (read-only) |

### 7.4. Frontend loyiha strukturasi

```
pharm-web/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
├── public/
│   └── favicon.ico
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/                    # Axios instance + API methods
    │   ├── axios.ts            # Interceptor (JWT auto-refresh)
    │   ├── auth.api.ts
    │   ├── users.api.ts
    │   ├── customers.api.ts
    │   ├── orders.api.ts
    │   └── ...
    ├── stores/                 # Pinia stores
    │   ├── auth.store.ts
    │   ├── order.store.ts
    │   └── ...
    ├── router/
    │   └── index.ts            # Route guards (role-based)
    ├── layouts/
    │   ├── AdminLayout.vue
    │   ├── CourierLayout.vue
    │   ├── OperatorLayout.vue
    │   └── ViewerLayout.vue
    ├── pages/
    │   ├── LoginPage.vue
    │   ├── admin/
    │   ├── operator/
    │   ├── courier/
    │   └── viewer/
    ├── components/
    │   ├── common/             # DataTable, Modal, ConfirmDialog...
    │   ├── orders/             # OrderForm, OrderStatusBadge...
    │   ├── customers/
    │   └── charts/             # Dashboard charts
    ├── composables/            # useAuth, usePagination, useNotification
    ├── types/                  # TypeScript interfaces
    ├── utils/                  # Formatters, validators
    └── assets/
        └── styles/
```

### 7.5. UI/UX yaxshilashlar (eski tizimga nisbatan)

| Eski | Yangi |
|------|-------|
| Sahifa reload (Thymeleaf) | SPA — instant navigation |
| Modal ichida CRUD | To'liq sahifalar + slide-over panels |
| DataTables (jQuery) | Vue DataTable (sort, filter, pagination server-side) |
| Inline Vue createApp | Pinia store + Composition API |
| Bootstrap 4 admin template | Zamonaviy UI kit |
| Faqat desktop | Mobile-first (kuryer paneli) |
| Rus tilida | O'zbek + Rus (i18n) |
| Status dropdown (modal) | Status timeline + quick actions |
| Alohida comment modal | Inline comment + image upload |
| Morris.js charts | ApexCharts / Chart.js |
| Yandex map (admin/address) | Yandex Maps Vue component |

---

## 8. Infrastruktura va deployment

### 8.1. Development muhiti

```
┌─────────────┐     ┌──────────────┐     ┌────────────┐
│  pharm-web  │────▶│  pharm-api   │────▶│ PostgreSQL │
│  :5173      │     │  :8080       │     │  :5432     │
│  (Vite dev) │     │  (Spring)    │     │  (mavjud)  │
└─────────────┘     └──────┬───────┘     └────────────┘
                           │
                    ┌──────▼───────┐
                    │    Redis     │
                    │    :6379     │
                    └──────────────┘
```

### 8.2. Production muhiti

```
┌──────────┐     ┌─────────────┐     ┌──────────────┐     ┌────────────┐
│  Nginx   │────▶│  pharm-web  │     │  pharm-api   │────▶│ PostgreSQL │
│  :80/443 │     │  (static)   │     │  :8080       │     │  (mavjud)  │
│          │────▶│             │     │              │     └────────────┘
│  /api/* ─│─────────────────────────▶│              │
└──────────┘     └─────────────┘     └──────┬───────┘
                                             │
                                      ┌──────▼───────┐
                                      │    Redis     │
                                      └──────────────┘
```

### 8.3. Environment variables

```env
# pharm-api
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://172.10.10.51:5432/velto_test_db
DB_USERNAME=postgres
DB_PASSWORD=***
JWT_SECRET=***
REDIS_HOST=localhost
REDIS_PORT=6379
UPLOAD_DIR=/app/uploads
CORS_ORIGINS=https://pharm.example.com

# pharm-web
VITE_API_BASE_URL=https://pharm.example.com/api/v1
VITE_YANDEX_MAPS_API_KEY=***
```

### 8.4. Database ulanish (prod)

```yaml
# pharm-api application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://172.10.10.51:5432/velto_test_db
    username: postgres
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate   # ← MUHIM: update EMAS!
```

---

## 9. Migratsiya strategiyasi

### 9.1. Bosqichlar

```
Bosqich 1: Backend API (2-3 hafta)
  ├── Entity/Repository nusxalash
  ├── Auth (JWT)
  ├── CRUD API (users, customers, categories, products)
  ├── Orders API (eng murakkab biznes logika)
  ├── Courier stock + Cash register API
  ├── Statistics API
  └── Swagger docs + test

Bosqich 2: Frontend Admin (2-3 hafta)
  ├── Login + Auth flow
  ├── Layout + Navigation
  ├── Dashboard
  ├── Users, Customers, Categories, Products CRUD
  ├── Store management
  └── Orders (list + create + detail)

Bosqich 3: Frontend Courier + Viewer (1-2 hafta)
  ├── Courier dashboard, orders, stock, cash
  ├── Viewer read-only panels
  └── Mobile responsive

Bosqich 4: Frontend Admin qolgan modullar (1-2 hafta)
  ├── Courier stock transfer/return
  ├── Cash register management
  ├── Statistics/reports
  └── Sale logs

Bosqich 5: OPERATOR + Polish (1 hafta)
  ├── Operator panel
  ├── i18n (O'zbek/Rus)
  ├── Bug fixes
  └── Performance optimization

Bosqich 6: Production deployment
  ├── Parallel run (eski + yangi)
  ├── Ma'lumotlar tekshiruvi
  ├── DNS switch
  └── Eski tizimni o'chirish
```

### 9.2. Parallel ishlash davri

Eski va yangi tizim **bir xil bazadan** o'qiydi. Yozish vaqtida:
- **Variant A (tavsiya):** Faqat yangi tizim orqali yozish, eski tizim read-only
- **Variant B:** Ikkalasi parallel, lekin conflict riski bor

### 9.3. File migration

Mavjud upload fayllar (`uploads/` papka) yangi API serverga ko'chirilishi kerak:
- Product images
- Delivery comment images

---

## 10. Testing strategiyasi

### 10.1. Backend

| Test turi | Tool | Qamrov |
|-----------|------|--------|
| Unit test | JUnit 5 + Mockito | Service layer biznes logika |
| Integration test | @SpringBootTest + Testcontainers | API endpoint + DB |
| Auth test | MockMvc | JWT flow |

**Muhim test senariylari:**
- Buyurtma yaratish (stock validation)
- Status o'zgarish (DELIVERED → cash + sale log)
- Cancel → stock qaytarish
- Courier stock transfer (ombor yetarli/emas)
- VIEWER DELETED buyurtmalarni ko'rmasligi
- Role-based access denial

### 10.2. Frontend

| Test turi | Tool |
|-----------|------|
| Component test | Vitest + Vue Test Utils |
| E2E test | Playwright yoki Cypress |

---

## 11. Xavfsizlik talablari

| Talab | Hozirgi | Yangi |
|-------|---------|-------|
| JWT secret | Hardcoded | Environment variable |
| Password | BCrypt | BCrypt (saqlanadi) |
| CORS | Yo'q (same origin) | Frontend domain whitelist |
| CSRF | Disabled | Kerak emas (JWT, stateless) |
| Rate limiting | Yo'q | Login endpoint (tavsiya) |
| Input validation | Minimal | Jakarta Validation (barcha DTO) |
| SQL injection | JPA (safe) | JPA (safe) |
| File upload | 30MB limit | 30MB + MIME type check |
| HTTPS | - | Production da majburiy |

---

## 12. Ma'lumotlar bazasiga ulanish — checklist

Yangi loyiha ishga tushirishdan oldin:

- [ ] PostgreSQL ga ulanish test qilingan
- [ ] `ddl-auto: validate` — schema mos kelishi tekshirilgan
- [ ] Barcha 15 jadval mavjudligi tasdiqlangan
- [ ] Enum qiymatlari mos kelishi tekshirilgan (STRING vs ORDINAL)
- [ ] Mavjud 5000+ buyurtma API orqali o'qilishi test qilingan
- [ ] Mavjud foydalanuvchilar bilan login test qilingan
- [ ] Upload papka (`uploads/`) yangi serverda mavjud
- [ ] Redis ulanishi ishlayapti
- [ ] `_users` jadvalidagi BCrypt parollar yangi auth bilan ishlaydi

---

## 13. Qarorlar kutilmoqda (Decision log)

Quyidagi savollar loyiha boshlanishidan oldin hal qilinishi kerak:

| # | Savol | Variantlar | Tavsiya |
|---|-------|------------|---------|
| 1 | OPERATOR roli huquqlari | To'liq spec (3.2) yoki ADMIN kabi | 3.2 dagi cheklangan huquq |
| 2 | Frontend framework | Vue 3 / React | Vue 3 (jamoa tajribasi) |
| 3 | UI Kit | PrimeVue / Vuetify / Naive UI | PrimeVue (DataTable kuchli) |
| 4 | Til | O'zbek + Rus / faqat Rus | O'zbek + Rus |
| 5 | Mobile app | Web responsive / alohida React Native | Web responsive (hozircha) |
| 6 | Eski tizim | O'chirish / parallel saqlash | Parallel → switch → o'chirish |
| 7 | Flyway | Yoqish / validate only | validate only (hozircha) |
| 8 | Yangi loyiha papkasi | `pharm-v2/` / alohida repo | `pharm-v2/` (monorepo) |

---

## 14. Hozirgi API → Yangi API mapping

Eski endpointlardan yangilariga o'tish jadvali (backward reference):

| Eski endpoint | Yangi endpoint |
|---------------|----------------|
| `POST /login` | `POST /api/v1/auth/login` |
| `GET /api/dashboard/statistic` | `GET /api/v1/statistics/dashboard` |
| `GET /api/statistics/daily` | `GET /api/v1/statistics/daily` |
| `GET /api/customers/search?phone=` | `GET /api/v1/customers/search?phone=` |
| `GET /api/products` | `GET /api/v1/products/in-stock` |
| `GET /api/orders` | `GET /api/v1/orders` |
| `PUT /api/orders/{id}/status` | `PATCH /api/v1/orders/{id}/status` |
| `GET /api/orders/{id}/detail` | `GET /api/v1/orders/{id}` |
| `POST /admin/orders/save` | `POST /api/v1/orders` |
| `POST /admin/users/save` | `POST /api/v1/users` |
| `GET /admin/users/couriers` | `GET /api/v1/users/couriers` |
| `GET /api/couriers/list` | `GET /api/v1/users/couriers` |
| `POST /api/courier/transfer` | `POST /api/v1/courier-stock/transfer` |
| `POST /api/courier/return` | `POST /api/v1/courier-stock/return` |
| `GET /api/courier/stock/{courierId}` | `GET /api/v1/courier-stock/{courierId}` |
| `GET /api/sale/log` | `GET /api/v1/sale-logs` |
| `GET /api/mobile/courier/*` | `GET /api/v1/courier/*` (unified) |

---

## 15. Xulosa

Bu TZ hujjati **PHARM** tizimining backend va frontendini ajratib qayta qurish uchun to'liq yo'l xaritasini beradi. Asosiy tamoyil — **mavjud database va biznes logikani buzmasdan**, zamonaviy REST API va SPA frontend yaratish.

**Keyingi qadam:** `pharm-v2/` papkasini yaratish va Bosqich 1 (Backend API) dan boshlash.
