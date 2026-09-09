# PHARM v2

Backend (Spring Boot REST API) va frontend (Vue 3 SPA) bitta monorepo ichida.

```
pharm2/
├── backend/     # Spring Boot 4 + JWT + JPA → mavjud PostgreSQL
└── frontend/    # Vue 3 + Vite + Pinia + PrimeVue
```

## Muhim qoidalar

- Database strukturasi **o‘zgarmaydi** (`ddl-auto: validate`)
- Entity/jadval nomlari eski loyiha bilan bir xil
- Mavjud bazaga ulanib, ishni davom ettirish mumkin

## Backend ishga tushirish

```bash
cd backend
# application-dev.yml da DB URL/user/password ni sozlang
mvn spring-boot:run
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Auth: `POST /api/v1/auth/login`

## Frontend ishga tushirish

```bash
cd frontend
npm install
npm run dev
```

- UI: http://localhost:5173
- Vite proxy `/api` → `http://localhost:8080`

## Hozirgi holat (Bosqich 1)

**Backend:**
- Entity/repository/service qatlami (eski DB bilan mos)
- JWT auth (`/api/v1/auth/*`)
- Users API (`/api/v1/users/*`)
- Orders API (`/api/v1/orders/*`)
- Statistics API (`/api/v1/statistics/*`)

**Frontend:**
- Login + role-based routing
- Admin: dashboard, buyurtmalar, users placeholder
- Operator / Courier / Viewer: skeleton

## Keyingi qadamlar

1. Customers, Categories, Products, Store API
2. Courier stock + Cash register API
3. Admin CRUD sahifalari
4. Courier / Viewer / Operator panellari
5. Production deploy (Nginx + jar)

Batafsil TZ: `../pharm/docs/TZ-YANGI-API-FRONTEND.md`
