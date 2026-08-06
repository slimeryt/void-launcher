# Polar Accounts API

Cloudflare Worker + D1 for Polar accounts and **Developer Beta** enrollment (request → admin approve).

## Setup

```bash
cd workers/polar-accounts
npm install

# Create D1 DB (once)
npx wrangler d1 create polar-accounts
# Paste the printed database_id into wrangler.jsonc

# Apply migrations
npm run db:remote

# Admin secret (used by /admin + X-Admin-Token)
npx wrangler secret put ADMIN_TOKEN

# Deploy
npm run deploy
```

Local:

```bash
npm run db:local
npm run dev
```

## Endpoints

| Method | Path | Auth |
|---|---|---|
| POST | `/v1/auth/register` | — body `{ email, password, displayName? }` |
| POST | `/v1/auth/login` | — body `{ email, password }` |
| POST | `/v1/auth/logout` | Bearer |
| GET | `/v1/me` | Bearer |
| POST | `/v1/developer/request` | Bearer — apply for Developer Account |
| POST | `/v1/enroll/request` | Bearer — enroll in Developer Beta (requires approved Developer Account) |
| GET | `/v1/enroll/status` | Bearer |
| GET | `/admin` | HTML UI (token in page) |
| GET | `/v1/admin/developer-accounts?status=pending` | `X-Admin-Token` |
| POST | `/v1/admin/developer-accounts/:userId/approve` | `X-Admin-Token` |
| POST | `/v1/admin/developer-accounts/:userId/deny` | `X-Admin-Token` |

Flow:

1. Create a normal Polar account  
2. **Become a Developer** → admin approves at `/admin`  
3. **Enroll in Developer Beta** (self-serve) → unlocks Developer channel in the app  

Register/login responses include:

```json
{
  "token": "…",
  "user": {
    "email": "you@example.com",
    "developerAccountStatus": "none",
    "isDeveloperAccount": false,
    "enrollmentStatus": "none",
    "developerEnrolled": false
  }
}
```

## Android

The app defaults to:

`https://polar-accounts.slimer0935.workers.dev`

Override with Gradle: `-PaccountApiBase=https://your-url`
