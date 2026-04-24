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

## Chạy ứng dụng

### 1. Clone và cấu hình môi trường

```powershell
git clone <repo-url>
cd SocialManager
Copy-Item .env.example .env
```

Mở `.env` và điền các giá trị thực:

- `JWT_SECRET` - chuỗi ngẫu nhiên tối thiểu 64 ký tự
- `AES_SECRET` - chuỗi chính xác 32 ký tự
- API key cho Gemini, Leonardo.ai, Cloudinary, Meta, TikTok

### 2. Chạy backend

Cách nhanh nhất là dùng `local` profile, không cần PostgreSQL:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\social-manager-backend
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

Nếu bạn muốn chạy với PostgreSQL trong Docker:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager
docker compose up -d

cd .\social-manager-backend
$env:SPRING_PROFILES_ACTIVE='dev'
.\mvnw.cmd spring-boot:run
```

Backend chạy tại: http://localhost:8080

- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health

### 3. Chạy frontend

Mở terminal mới:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\frontend
npm.cmd install
npm.cmd run dev -- --port 3001
```

Frontend chạy tại: http://localhost:3001

### 4. Tài khoản local mặc định

- Username: `devuser`
- Password: `devpass123`

### 5. Kiểm tra nhanh toàn bộ stack

Từ thư mục gốc của repo:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager
.\smoke.ps1
```

Khi mọi thứ ổn, script sẽ in:

- `BACKEND_OK=True`
- `FRONTEND_OK=True`
- `AUTH_OK=True`
- `MONITOR_SUMMARY_OK=True`
- `MONITOR_RECENT_OK=True`
- `SMOKE_OK`

### 6. Kiểm tra thủ công từng phần

Mở 2 terminal: một terminal chạy backend `local`, một terminal chạy frontend như trên. Sau đó dùng terminal thứ 3 để test:

1) Health check backend

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing | Select-Object -ExpandProperty StatusCode
```

2) Login lấy JWT token

```powershell
$loginBody = @{ username='devuser'; password='devpass123' } | ConvertTo-Json
$login = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody -UseBasicParsing
$token = ($login.Content | ConvertFrom-Json).token
```

3) Gọi API cần auth với Bearer token

```powershell
$headers = @{ Authorization = "Bearer $token" }
$body = @{
	socialAccountId = "550e8400-e29b-41d4-a716-446655440000"
	content = "Local smoke test"
	mediaUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30"
	scheduledTime = "2026-12-31T10:00:00"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/posts/preview" -Method Post -Headers $headers -ContentType "application/json" -Body $body -UseBasicParsing
```

4) Kiểm tra monitor API

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/posts/monitor/summary" -Method Get -Headers $headers -UseBasicParsing
Invoke-WebRequest -Uri "http://localhost:8080/api/posts/monitor/recent" -Method Get -Headers $headers -UseBasicParsing
```

### 7. Verify autopost thật sự chạy

Phần này kiểm tra luồng autopost end-to-end trong `local` profile (Quartz memory mode).

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\social-manager-backend
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

Đăng nhập, tạo scheduled post với thời gian gần hiện tại, rồi theo dõi log backend và gọi lại monitor API. Khi chạy đúng, bạn sẽ thấy record chuyển từ `PENDING` sang `PROCESSING` rồi `POSTED` hoặc `FAILED`, và `publishedPostId` có giá trị nếu publish thành công.

### 8. Social account management API

- `GET /api/social-accounts` - liệt kê các account đã kết nối
- `GET /api/social-accounts/{id}` - xem chi tiết một account
- `PUT /api/social-accounts/{id}` - cập nhật token, tên, alias, trạng thái autopilot
- `PATCH /api/social-accounts/{id}/autopilot?enabled=true|false` - bật/tắt autopilot nhanh
- `DELETE /api/social-accounts/{id}` - gỡ account khỏi hệ thống

Các endpoint này lấy theo user đang đăng nhập. Trong `local` profile, demo social account được seed sẵn:

- `socialAccountId = 550e8400-e29b-41d4-a716-446655440000`
- `accountName = Local Facebook Demo`

### 9. Build production

```powershell
# Backend JAR
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\social-manager-backend
.\mvnw.cmd clean package -DskipTests

# Frontend build
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\frontend
npm.cmd run build
```

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
│       ├── controller/        # AuthController, PostController
│       ├── model/             # JPA entities
│       ├── repository/        # Spring Data repositories
│       ├── dto/               # Request/Response DTOs
│       ├── exception/         # GlobalExceptionHandler
│       └── util/              # JwtUtil, EncryptionUtil (AES-GCM)
├── docker-compose.yml
├── .env.example
├── smoke.ps1
└── README.md
```