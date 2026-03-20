# Free Stack

This repo is prepared for a low-cost public demo stack:

- Backend: Render free web service
- PostgreSQL: Neon free plan
- Redis: Upstash Redis free plan
- Media storage: Cloudflare R2 free tier

## Why This Stack

- Render is the simplest free host for a Dockerized Ktor service.
- Neon gives a real managed Postgres database.
- Upstash gives a managed Redis URL that works with Lettuce.
- R2 gives S3-compatible storage without running MinIO.

Important tradeoff:

- Render free services can spin down after inactivity, so the first request may be slow.
- This is acceptable for a demo, but not ideal for production messaging.

## Accounts To Create

1. Render
2. Neon
3. Upstash
4. Cloudflare with R2 enabled

## Values To Collect

### Neon

- Connection string in JDBC form, or plain Postgres URL plus user/password

Recommended:

- `DB_URL=jdbc:postgresql://...`
- `DB_USER=...`
- `DB_PASSWORD=...`
- `DB_SSL_MODE=require`

### Upstash

- `REDIS_URL=rediss://default:password@host:port`

### Cloudflare R2

- Bucket name
- S3 endpoint, for example `https://<accountid>.r2.cloudflarestorage.com`
- Access key ID
- Secret access key

Use:

- `MINIO_ENDPOINT=https://<accountid>.r2.cloudflarestorage.com`
- `MINIO_REGION=auto`
- `MINIO_ACCESS_KEY=...`
- `MINIO_SECRET_KEY=...`
- `MINIO_BUCKET=messenger-media`
- `S3_PATH_STYLE=false`
- `S3_AUTO_CREATE_BUCKET=false`

## Render Deploy

1. Push this repo to GitHub.
2. In Render, create a new Blueprint and point it to the repo.
3. Render will read [render.yaml](/c:/Users/User/Desktop/messenger/render.yaml).
4. Fill in the `sync: false` environment variables from Neon, Upstash, and R2.
5. Set `PUBLIC_BASE_URL` to the final Render URL, for example `https://messenger-server.onrender.com`.
6. Deploy.

## Minimum Working Env

```env
APP_ENV=production
SERVER_HOST=0.0.0.0
PUBLIC_BASE_URL=https://your-service.onrender.com

DB_URL=jdbc:postgresql://ep-...neon.tech/neondb?sslmode=require
DB_USER=your_user
DB_PASSWORD=your_password
DB_SSL_MODE=require

REDIS_URL=rediss://default:password@host:port

JWT_SECRET=replace-with-long-random-secret

MINIO_ENDPOINT=https://<accountid>.r2.cloudflarestorage.com
MINIO_REGION=auto
MINIO_ACCESS_KEY=your_r2_access_key
MINIO_SECRET_KEY=your_r2_secret_key
MINIO_BUCKET=messenger-media
S3_PATH_STYLE=false
S3_AUTO_CREATE_BUCKET=false

SMTP_HOST=
SMTP_PORT=587
SMTP_USER=
SMTP_PASSWORD=
SMTP_FROM=

TOTP_ISSUER=Messenger
STUN_URL=stun:stun.l.google.com:19302
```

## Verify After Deploy

1. Open `/health`
2. Open `/metrics`
3. Register a user through the API
4. Upload a file and open the returned `/media/...` URL

## Current Limits

- Render free web services may sleep after inactivity.
- Neon free is good for development and demos.
- Upstash free is suitable for a light demo workload.
- R2 free tier is generous for small media usage.

If you need a no-sleep deployment, move the same server to a paid VPS or paid Render plan without changing app code.
