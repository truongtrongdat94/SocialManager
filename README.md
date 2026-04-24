# Social Manager

Nền tảng quản lý tài khoản mạng xã hội cá nhân — hỗ trợ Facebook Pages, Instagram Business, Threads và TikTok.

## Tính năng chính

- Kết nối OAuth cho Meta (Facebook/Instagram/Threads) và TikTok
- Lên lịch đăng bài với giao diện calendar
- Tạo nội dung AI — caption qua Gemini, ảnh qua Leonardo.ai, lưu trữ trên Cloudinary
- Dashboard analytics với 6 biểu đồ Recharts
- AI Auto Pilot (chỉ Facebook Pages) — tự động generate và đăng bài theo từ khóa

## Tech Stack

| Layer | Stack |
|---|---|
| Frontend | React 18 + Vite + TypeScript + Recharts |
| Backend | Java 17 + Spring Boot 4 + Spring Security + JPA |
| Database | PostgreSQL 16 |
| Scheduler | Quartz (JDBC/DB Store, clustered) |
| AI | Google Gemini + Leonardo.ai |
| Media | Cloudinary |

## Yêu cầu

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

---

## Khởi động

### 1. Clone và cấu hình môi trường

```bash
git clone <repo-url>
cd SocialManager

# Copy file env mẫu và điền giá trị thực
cp .env.example .env
```

Mở `.env` và điền các giá trị:
- `JWT_SECRET` — chuỗi ngẫu nhiên tối thiểu 64 ký tự
- `AES_SECRET` — chuỗi chính xác 32 ký tự
- Các API key: Gemini, Leonardo.ai, Cloudinary, Meta, TikTok

### 2. Khởi động PostgreSQL

```bash
docker compose up -d
```

Kiểm tra database đã sẵn sàng:
```bash
docker compose ps
```

### 3. Chạy Backend

```bash
cd social-manager-backend
mvn spring-boot:run
```

Backend chạy tại: http://localhost:8080

- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health

### 4. Chạy Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend chạy tại: http://localhost:3000

---

## Kiểm thử

### Backend tests

```bash
cd social-manager-backend
.\mvnw test
```

Lệnh này chạy toàn bộ bộ test Java với profile `test`.

### Frontend build check

```bash
cd frontend
npm run build
```

Lệnh này kiểm tra TypeScript và build production của frontend.

---

## Cấu trúc project

```
SocialManager/
├── frontend/                  # React 18 + Vite
│   └── src/
│       ├── api/               # Axios instance + interceptors
│       └── pages/             # Route pages
├── social-manager-backend/    # Spring Boot
│   └── src/main/java/com/socialmanager/
│       ├── config/            # QuartzConfig, SecurityConfig
│       ├── model/             # JPA entities (8 tables)
│       ├── repository/        # Spring Data repositories
│       ├── dto/               # Request/Response DTOs
│       ├── exception/         # GlobalExceptionHandler
│       └── util/              # JwtUtil, EncryptionUtil (AES-GCM)
├── docker-compose.yml
├── .env.example
└── README.md
```

## Build production

```bash
# Backend JAR
cd social-manager-backend
mvn clean package -DskipTests

# Frontend build
cd frontend
npm run build
```
