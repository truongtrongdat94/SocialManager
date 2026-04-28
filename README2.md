# README2 - Posting + AI-Wired Posting Test Guide

# Stuff that actually matters

cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager
docker compose up -d

docker ps

cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\social-manager-backend
$env:SPRING_PROFILES_ACTIVE='dev'

$env:FACEBOOK_CLIENT_ID="2023200831629939"
$env:FACEBOOK_CLIENT_SECRET="780e710545cdbcbb6385a5941a76e488"
$env:FACEBOOK_REDIRECT_URI="http://localhost:8080/callback"
$env:FACEBOOK_GRAPH_API_VERSION="v22.0"

$env:INSTAGRAM_CLIENT_ID="test"
$env:INSTAGRAM_CLIENT_SECRET="test"
$env:INSTAGRAM_REDIRECT_URI="http://localhost:8080/api/auth/instagram/callback"

$env:TIKTOK_CLIENT_ID="test"
$env:TIKTOK_CLIENT_SECRET="test"
$env:TIKTOK_CLIENT_KEY="your_value_here"
$env:TIKTOK_REDIRECT_URI="http://localhost:8080/api/auth/instagram/callback"

$env:THREADS_CLIENT_ID="your_value"
$env:THREADS_CLIENT_SECRET="your_value"
$env:THREADS_REDIRECT_URI="your_value"


$env:JWT_SECRET="test"

docker exec -it social-manager-postgres psql -U postgres -d social_manager

SELECT * FROM scheduled_posts;

SELECT gen_random_uuid();

INSERT INTO users (
    id,
    created_at,
    email,
    name,
    password,
    updated_at,
    username
)
VALUES (
    'a6732af2-3f62-4125-9011-3141c025d4e0',
    NOW(),
    'test@example.com',
    'Test User',
    'fake_password',
    NOW(),
    'testuser'
);

INSERT INTO social_accounts (
    id,
    platform,
    account_name,
    access_token,
    is_auto_pilot,
    created_at,
    user_id
)
VALUES (
    gen_random_uuid(),
    'FACEBOOK',
    'Test Facebook Account',
    'fake_test_token',
    true,
    NOW(),
    'a6732af2-3f62-4125-9011-3141c025d4e0'
);

INSERT INTO scheduled_posts (
    caption,
    created_at,
    is_auto_pilot,
    retry_count,
    scheduled_time,
    status,
    social_account_id,
    user_id
)
VALUES (
    'Quartz test post',
    NOW(),
    true,
    0,
    NOW() - INTERVAL '1 minute',
    'PENDING',
    '0be89d0a-8659-48ac-b35c-43693d879504',
    'a6732af2-3f62-4125-9011-3141c025d4e0'
);

INSERT INTO scheduled_posts (
    id,
    caption,
    created_at,
    is_auto_pilot,
    retry_count,
    scheduled_time,
    status,
    social_account_id,
    user_id
)
VALUES (
    gen_random_uuid(),
    'Quartz test post',
    NOW(),
    true,
    0,
    NOW() - INTERVAL '1 minute',
    'PENDING',
    '0be89d0a-8659-48ac-b35c-43693d879504',
    'a6732af2-3f62-4125-9011-3141c025d4e0'
);

.\mvnw.cmd spring-boot:run

# Ignore dumb stuff below

This guide explains exactly how to verify:
1. Posting works properly
2. Posting wired with Content AI sources works

It includes:
- UI test flow
- API test flow (PowerShell)
- Expected results per step

## 1. Pre-Requirements

- Java 17+
- Node.js 18+
- Frontend dependencies installed
- Backend dependencies installed through `mvnw`

## 2. Start The App (Local Quick Path)

### 2.1 Start backend (local profile)

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\social-manager-backend
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

Expected:
- Backend up at `http://localhost:8080`
- Local profile seeds demo user/account

### 2.2 Start frontend

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\frontend
npm.cmd install
npm.cmd run dev -- --port 3001
```

Expected:
- Frontend up at `http://localhost:3001`

## 3. Test A - Posting Works Properly

## 3.1 Login and get token (API)

```powershell
$loginBody = @{ username='devuser'; password='devpass123' } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $loginRes.data.token
$headers = @{ Authorization = "Bearer $token" }
$token
```

Expected:
- `$token` is not empty

## 3.2 Verify social account exists

```powershell
$accounts = Invoke-RestMethod -Uri "http://localhost:8080/api/social-accounts" -Headers $headers
$accounts.data
```

Expected:
- At least one account exists (local demo account)

## 3.3 Preview a post

Use a media URL with file extension (`.png`, `.jpg`, `.mp4`) so auto-post media validation can pass.

```powershell
$socialAccountId = $accounts.data[0].id
$previewBody = @{
  socialAccountId = $socialAccountId
  content = 'E2E manual posting preview test'
  mediaUrl = 'https://cdn.example.com/test-image.png'
  scheduledTime = (Get-Date).AddMinutes(1).ToString('yyyy-MM-ddTHH:mm:ss')
} | ConvertTo-Json

$previewRes = Invoke-RestMethod -Uri "http://localhost:8080/api/posts/preview" -Method Post -Headers $headers -ContentType "application/json" -Body $previewBody
$previewRes
```

Expected:
- `success = true`
- `data.status = PENDING`

## 3.4 Schedule a post

```powershell
$scheduleBody = @{
  socialAccountId = $socialAccountId
  content = 'E2E manual posting schedule test'
  mediaUrl = 'https://cdn.example.com/test-image.png'
  scheduledTime = (Get-Date).AddSeconds(15).ToString('yyyy-MM-ddTHH:mm:ss')
} | ConvertTo-Json

$scheduleRes = Invoke-RestMethod -Uri "http://localhost:8080/api/posts/schedule" -Method Post -Headers $headers -ContentType "application/json" -Body $scheduleBody
$scheduleRes
```

Expected:
- `success = true`
- returns scheduled post id in `data.id`

## 3.5 Check monitor transitions

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/posts/monitor/summary" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/posts/monitor/recent" -Headers $headers
```

Wait ~20 seconds and run again.

Expected:
- queued post transitions from `PENDING` -> `POSTED` (or `FAILED` if invalid media/logic)
- recent list includes post id and status

## 3.6 Check history endpoint

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/posts/history?page=0&size=10" -Headers $headers
```

Expected:
- `success = true`
- `data.items` includes newly scheduled post

## 4. Test B - Posting Wired To AI (Route-Level)

This test validates that AI-wired endpoints work even without existing AI source records.

## 4.1 AI preview using contentOverride only

```powershell
$aiPreviewBody = @{
  socialAccountId = $socialAccountId
  scheduledTime = (Get-Date).AddMinutes(2).ToString('yyyy-MM-ddTHH:mm:ss')
  contentOverride = 'AI route wiring test via contentOverride'
  mediaUrl = 'https://cdn.example.com/test-image.png'
} | ConvertTo-Json

$aiPreviewRes = Invoke-RestMethod -Uri "http://localhost:8080/api/posts/ai/preview" -Method Post -Headers $headers -ContentType "application/json" -Body $aiPreviewBody
$aiPreviewRes
```

Expected:
- `success = true`
- `data.contentSource = REQUEST_OVERRIDE`
- `data.post` is present

## 4.2 AI schedule using contentOverride only

```powershell
$aiScheduleBody = @{
  socialAccountId = $socialAccountId
  scheduledTime = (Get-Date).AddSeconds(20).ToString('yyyy-MM-ddTHH:mm:ss')
  contentOverride = 'AI route schedule wiring test via contentOverride'
  mediaUrl = 'https://cdn.example.com/test-image.png'
} | ConvertTo-Json

$aiScheduleRes = Invoke-RestMethod -Uri "http://localhost:8080/api/posts/ai/schedule" -Method Post -Headers $headers -ContentType "application/json" -Body $aiScheduleBody
$aiScheduleRes
```

Expected:
- `success = true`
- `data.post.status = PENDING`
- then monitor should move it to `POSTED`/`FAILED`

## 5. Test C - True AI Source Wiring (Using aiGenerationLogId/imageGenerationId)

To validate real source-ID wiring, create source rows in DB and call AI endpoints with those IDs.

Important:
- Local profile uses in-memory H2 schema setup that does not guarantee AI tables.
- Use dev profile + PostgreSQL for this test.

## 5.1 Start PostgreSQL and backend dev profile

```powershell
cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager
docker compose up -d

cd D:\00_Tai_lieu_DH\4.2_Java\Project\SocialManager\social-manager-backend
$env:SPRING_PROFILES_ACTIVE='dev'
.\mvnw.cmd spring-boot:run
```

## 5.2 Login as a user that already has a connected social account

```powershell
$loginBody = @{ username='your_existing_username'; password='your_password' } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $loginRes.data.token
$headers = @{ Authorization = "Bearer $token" }

$me = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Headers $headers
$userId = $me.data.id
$userId

$accounts = Invoke-RestMethod -Uri "http://localhost:8080/api/social-accounts" -Headers $headers
if (-not $accounts.data -or $accounts.data.Count -eq 0) {
  throw "No connected social account for this user. Connect one first via UI (/login -> Connect Facebook/TikTok)."
}

$socialAccountId = $accounts.data[0].id
$socialAccountId
```

## 5.3 Insert AI source rows into PostgreSQL

```powershell
$insertSql = @"
insert into ai_generation_logs (id, user_id, prompt, result_caption, image_urls, created_at)
values (
  gen_random_uuid(),
  '$userId',
  'test prompt',
  'Caption from ai_generation_logs',
  ARRAY['https://cdn.example.com/from-ai-log.png'],
  now()
);

insert into image_generations (id, user_id, prompt, caption, status, cloudinary_urls, cloudinary_public_ids, created_at)
values (
  gen_random_uuid(),
  '$userId',
  'image prompt',
  'Caption from image_generations',
  'COMPLETE',
  ARRAY['https://cdn.example.com/from-image-generation.png'],
  ARRAY['public-id-1'],
  now()
);
"@

$insertSql | docker exec -i social-manager-postgres psql -U postgres -d social_manager
```

## 5.4 Read AI sources API

```powershell
$sources = Invoke-RestMethod -Uri "http://localhost:8080/api/posts/ai/sources?limit=20" -Headers $headers
$sources
```

Expected:
- `sources.data.aiGenerationLogs` has records
- `sources.data.imageGenerations` has records

## 5.5 Call AI preview/schedule with source IDs

```powershell
$aiLogId = $sources.data.aiGenerationLogs[0].id
$imageGenId = $sources.data.imageGenerations[0].id

$aiFromSourceBody = @{
  socialAccountId = $socialAccountId
  scheduledTime = (Get-Date).AddMinutes(3).ToString('yyyy-MM-ddTHH:mm:ss')
  aiGenerationLogId = $aiLogId
  imageGenerationId = $imageGenId
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/posts/ai/preview" -Method Post -Headers $headers -ContentType "application/json" -Body $aiFromSourceBody
Invoke-RestMethod -Uri "http://localhost:8080/api/posts/ai/schedule" -Method Post -Headers $headers -ContentType "application/json" -Body $aiFromSourceBody
```

Expected:
- `contentSource` shows AI source type (for example `AI_GENERATION_LOG`)
- `resolvedMediaUrls` populated from AI rows if not overridden
- scheduled post appears in monitor/history

## 6. Frontend UI Verification Checklist

In `http://localhost:3001/posts` verify:
- Compose Mode switch appears (Manual / AI-assisted)
- AI-assisted mode shows:
  - `Refresh AI sources`
  - `AI Caption Source`
  - `AI Image Source`
- Preview and Schedule both work in:
  - Manual mode
  - AI-assisted mode
- Monitor panel updates after schedule
- History panel contains created posts

## 7. Pass Criteria Summary

Posting is considered working when:
- `/api/posts/preview` and `/api/posts/schedule` return success
- monitor and history endpoints return expected data
- queued posts transition to terminal status (`POSTED` or `FAILED`) predictably

AI-wired posting is considered working when:
- `/api/posts/ai/preview` and `/api/posts/ai/schedule` return success
- route-level test works with `contentOverride`
- source-level test works with `aiGenerationLogId`/`imageGenerationId`
- API returns `contentSource` and `resolvedMediaUrls` correctly

## 8. Troubleshooting

- 401 Unauthorized:
  - Token missing/expired, login again

- AI sources endpoint empty:
  - No AI source records for current user
  - Seed records in PostgreSQL as shown above

- Auto-post stays pending:
  - Check scheduler logs
  - Ensure media URL includes supported extension

- Local monitor test fails randomly:
  - Re-run with a future schedule time (15-30 seconds)
  - Wait for next cron tick (`app.posting.cron` in local is every 10s)
