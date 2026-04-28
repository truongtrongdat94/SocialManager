# Social Manager Manual Setup Guide

This guide is the manual setup path for the current SocialManager workspace. Use it when you want to bring the app up yourself without relying on Copilot or any helper script.

## 1. Prerequisites

Install these first:

- Java 17+
- Node.js 18+
- Maven 3.8+
- Docker and Docker Compose if you want the PostgreSQL-based dev profile

## 2. Get the project ready

Open a terminal in the repository root:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager
```

If the repo uses an environment template, copy it first and fill in the real values:

```powershell
Copy-Item .env.example .env
```

Set the required secrets and API keys in `.env` or in your shell environment:

- `JWT_SECRET`
- `AES_SECRET`
- Meta app credentials
- TikTok credentials if you use TikTok
- Gemini, Leonardo.ai, and Cloudinary keys if you use AI media generation

## 3. Start the backend

### Option A: local profile

Use this if you want the fastest start and do not need PostgreSQL in Docker:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\social-manager-backend
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

### Option B: dev profile with PostgreSQL

Use this if you want the full dev stack:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager
docker compose up -d

cd .\social-manager-backend
$env:SPRING_PROFILES_ACTIVE='dev'
.\mvnw.cmd spring-boot:run
```

The backend should be available at:

- `http://localhost:8080`
- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/actuator/health`

## 4. Start the frontend

Open a second terminal:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\frontend
npm.cmd install
npm.cmd run dev -- --port 3001
```

If port `3001` is busy, Vite may pick another port. The backend CORS setup already allows localhost on arbitrary ports.

## 5. Sign in

Use the local demo account if you are running the local profile:

- Username: `devuser`
- Password: `devpass123`

Then open the app in your browser and log in:

- `http://localhost:3001/login`

## 6. Connect a social account

Go to the dashboard and connect the platform you want to publish to.

For Facebook/Meta, make sure the Meta app is configured correctly:

- Your Facebook account must be an admin, developer, or tester for the app, or the app must be live.
- Facebook Login must be enabled.
- The redirect URI must include `http://localhost:8080/api/social-accounts/callback/facebook`.
- The app ID and app secret must be present in the backend environment.

## 7. Create and schedule a post

Open the post scheduler page and create a preview first, then schedule.

Use a media URL that the backend can recognize:

- Best case: a URL that ends in `.jpg`, `.jpeg`, `.png`, `.webp`, `.gif`, `.mp4`, `.mov`, or `.webm`
- Extensionless URLs can still work if the host returns a valid image or video content type

Example manual request flow:

1. Log in and copy the JWT from the login response if you are using API calls.
2. Call `GET /api/social-accounts` to get a connected account ID.
3. Call `POST /api/posts/preview` with `socialAccountId`, `content`, `mediaUrl`, and `scheduledTime`.
4. Call `POST /api/posts/schedule` once the preview looks right.

## 8. Verify the scheduler

After scheduling, check these endpoints:

- `GET /api/posts/monitor/summary`
- `GET /api/posts/monitor/recent`
- `GET /api/posts/history`

If the post fails, look at `errorMessage` in history and confirm the media URL is reachable from the backend host.

## 9. Common media URL issue

If you paste a Google thumbnail-style URL or another link with no extension, the app now tries content-type detection first.

That means:

- The URL may still work if the server responds with `image/*` or `video/*`
- The URL may still fail if the host blocks `HEAD` requests or does not expose a usable content type

If you want the most reliable result, use a direct media URL with a file extension.

## 10. Build checks

Run these when you want to verify the project manually:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\social-manager-backend
.\mvnw.cmd -q -DskipTests compile

cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\frontend
npm.cmd run build
```

## 11. Smoke test

From the repository root:

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager
.\smoke.ps1
```

If everything is configured correctly, the smoke test should report backend, frontend, auth, and monitor checks as OK.