# Free Stack

This repo is prepared for a low-cost public demo stack:

- Backend: Render free web service
- PostgreSQL: Neon free plan
- Redis: Upstash Redis free plan
- Media storage: local disk on Render for demo mode

## Why This Stack

- Render is the simplest free host for a Dockerized Ktor service.
- Neon gives a real managed Postgres database.
- Upstash gives a managed Redis URL that works with Lettuce.

Important tradeoff:

- Render free services can spin down after inactivity, so the first request may be slow.
- This is acceptable for a demo, but not ideal for production messaging.
- Local media files on Render are not durable. They can disappear after redeploys or restarts.

## Accounts To Create

1. Render
2. Neon
3. Upstash

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

## Render Deploy

1. Push this repo to GitHub.
2. In Render, create a new Blueprint and point it to the repo.
3. Render will read [render.yaml](/c:/Users/User/Desktop/messenger/render.yaml).
4. Fill in the `sync: false` environment variables from Neon and Upstash.
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

MEDIA_STORAGE_MODE=local
MEDIA_LOCAL_DIR=/opt/render/project/.render-media

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
- Local media storage is only for demo use.

If you need durable media later, switch `MEDIA_STORAGE_MODE` to `s3` and add S3-compatible credentials.
