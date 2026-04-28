# Springboot-url_shortner

This repository is a end-to-end URL shortener:

- **Backend**: Spring Boot + Spring Data JPA (Hibernate) + **H2 file DB**
- **Frontend**: React (Vite) — simple UI to shorten URLs

The implementation is intentionally minimal, but the README is written from a **system design perspective** (how it works now, and how you scale it later).

---

## What the system does

### Functional requirements (MVP)
- **Create** a short link from a long URL
- **Redirect** from `/{shortCode}` to the original long URL (HTTP **302**)
- **Optional alias**: user-provided short code (e.g. `my-link`)
- **Optional expiration**: link can expire at a specified time

### Non-functional goals (MVP)
- Simple and easy to run locally
- Correctness for alias uniqueness and expiration rules

---

## Current architecture (MVP)

This is a **single backend service** with a separate **React UI**.

**Flow: Create**
1. Client sends `POST /api/v1/shorten`
2. Backend checks if a valid (non-expired) mapping already exists for the same `longUrl`
3. If yes: returns the existing `shortCode`
4. If no: creates a new row and generates a short code using **base62(id)**
5. Returns `{ shortCode, shortUrl, ... }`

**Flow: Redirect**
1. Browser requests `GET /{shortCode}`
2. Backend looks up the mapping
3. If not found → **404**
4. If expired → **410**
5. Else → returns **302** redirect to the destination URL

---

## Data model

Single table (represented by JPA entity `UrlMapping`):

- `id` (auto-increment primary key)
- `longUrl` (original destination URL)
- `shortCode` (unique code exposed in the short URL path)
- `createdAt`
- `expiresAt` (nullable)

Notes:
- `shortUrl` is **derived** (constructed from `app.base.url + "/" + shortCode`) and is not stored.
- With H2 file DB, data persists under `./data/`.

---

## Short code generation strategy

### Current strategy (base62 of database id)
- Insert row → get `id`
- `shortCode = base62(id)`

Pros:
- Very simple and fast
- No collision retries for auto-generated codes

Cons:
- Predictable (sequential ids) unless you add a shuffling step
- Requires a write to get an `id` before computing `shortCode` (2-step save)

### Optional aliases
- If an alias is provided, it becomes the `shortCode`
- Backend enforces:
  - length **3–32**
  - only `[a-zA-Z0-9-]`
  - unique across all mappings

---

## Expiration rules

- A link is considered **expired** when `expiresAt < now`.
- On create:
  - If a non-expired mapping already exists for the same `longUrl`, it is reused.
  - If the latest mapping is expired, the service can create a new mapping.
- On redirect:
  - expired → **410 Gone**

---

## API

### Create short URL
`POST /api/v1/shorten`

Request JSON (no DTOs; backend accepts the entity shape):

```json
{
  "longUrl": "https://example.com/some/path",
  "alias": "my-link",
  "expiresAt": "2026-12-31T23:59:59"
}
```

- `longUrl` is required
- `alias` and `expiresAt` are optional

Response JSON (example):

```json
{
  "id": 1,
  "longUrl": "https://example.com/some/path",
  "shortCode": "b",
  "createdAt": "2026-04-27T15:08:25.8160811",
  "expiresAt": "2026-12-31T23:59:59",
  "shortUrl": "http://localhost:8084/b",
  "alias": null
}
```

### Redirect
`GET /{shortCode}`

- Success: HTTP **302** with `Location: <longUrl>`
- Not found: **404**
- Expired: **410**

---

## How to run locally

### Backend
From repo root:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend runs on `server.port` (default: `8084`).

### Frontend
From `frontend/`:

```powershell
npm install
npm run dev
```

Vite runs on `5173` and proxies `/api` to the backend.

---

## Scaling path (how you’d evolve this design)

If you want to move toward a Bitly-like design:

### 1) Split read/write services
- **Write Service**: create/alias/expiration
- **Read Service**: redirect only
- Lets you scale redirect traffic independently from creates.

### 2) Add caching for redirects
- Add Redis cache: `shortCode -> longUrl`
- Redirect service checks cache first, DB second
- TTL should align with expiration time

### 3) Improve uniqueness generation at scale
Options:
- **Redis counter** + base62 (global atomic counter)
- **Hash-based** codes (SHA/HMAC + base62 + collision retries)

### 4) Add observability + abuse controls
- Rate limiting (per IP / per API key)
- Logging + metrics (p95 latency, cache hit rate, error rate)
- Allowlist/denylist or malware scanning for destinations

---

## Repo structure

- `src/main/java/...` — Spring Boot backend
- `src/main/resources/application.properties` — backend config
- `frontend/` — React app (Vite)

