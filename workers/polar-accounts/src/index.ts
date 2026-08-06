import type {
  Env,
  DeveloperAccountStatus,
  EnrollmentRow,
  EnrollmentStatus,
  UserRow,
} from "./types";
import { hashPassword, randomId, randomToken, sha256Hex, verifyPassword } from "./crypto";

const SESSION_DAYS = 90;
const MIN_PASSWORD = 8;

const USER_SELECT =
  "id, email, password_hash, display_name, created_at, developer_status, developer_requested_at, developer_reviewed_at";

function json(data: unknown, status = 200, extraHeaders: HeadersInit = {}): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      ...corsHeaders(),
      ...extraHeaders,
    },
  });
}

function corsHeaders(): HeadersInit {
  return {
    "access-control-allow-origin": "*",
    "access-control-allow-methods": "GET, POST, OPTIONS",
    "access-control-allow-headers": "content-type, authorization, x-admin-token",
    "access-control-max-age": "86400",
  };
}

function error(message: string, status = 400): Response {
  return json({ error: message }, status);
}

function nowIso(): string {
  return new Date().toISOString();
}

function expiresIso(days: number): string {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString();
}

function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

async function readJson<T>(request: Request): Promise<T | null> {
  try {
    return (await request.json()) as T;
  } catch {
    return null;
  }
}

function bearerToken(request: Request): string | null {
  const h = request.headers.get("authorization") || "";
  const m = /^Bearer\s+(.+)$/i.exec(h.trim());
  return m?.[1]?.trim() || null;
}

function developerStatusOf(user: UserRow): DeveloperAccountStatus {
  const s = user.developer_status || "none";
  if (s === "pending" || s === "approved" || s === "denied") return s;
  return "none";
}

async function createSession(env: Env, userId: string): Promise<string> {
  const token = randomToken();
  const tokenHash = await sha256Hex(token);
  await env.DB.prepare(
    "INSERT INTO sessions (token_hash, user_id, expires_at) VALUES (?, ?, ?)"
  )
    .bind(tokenHash, userId, expiresIso(SESSION_DAYS))
    .run();
  return token;
}

async function userFromRequest(env: Env, request: Request): Promise<UserRow | null> {
  const token = bearerToken(request);
  if (!token) return null;
  const tokenHash = await sha256Hex(token);
  const row = await env.DB.prepare(
    `SELECT u.id, u.email, u.password_hash, u.display_name, u.created_at,
            u.developer_status, u.developer_requested_at, u.developer_reviewed_at
     FROM sessions s
     JOIN users u ON u.id = s.user_id
     WHERE s.token_hash = ? AND s.expires_at > ?`
  )
    .bind(tokenHash, nowIso())
    .first<UserRow>();
  return row ?? null;
}

async function getEnrollment(env: Env, userId: string): Promise<EnrollmentRow> {
  const row = await env.DB.prepare(
    "SELECT user_id, status, requested_at, reviewed_at, note FROM enrollments WHERE user_id = ?"
  )
    .bind(userId)
    .first<EnrollmentRow>();
  return (
    row ?? {
      user_id: userId,
      status: "none",
      requested_at: null,
      reviewed_at: null,
      note: "",
    }
  );
}

function mePayload(user: UserRow, enrollment: EnrollmentRow) {
  const developerAccountStatus = developerStatusOf(user);
  const isDeveloperAccount = developerAccountStatus === "approved";
  const enrollmentStatus = enrollment.status as EnrollmentStatus;
  return {
    id: user.id,
    email: user.email,
    displayName: user.display_name || "",
    createdAt: user.created_at,
    developerAccountStatus,
    isDeveloperAccount,
    developerRequestedAt: user.developer_requested_at ?? null,
    developerReviewedAt: user.developer_reviewed_at ?? null,
    enrollmentStatus,
    enrollmentRequestedAt: enrollment.requested_at,
    enrollmentReviewedAt: enrollment.reviewed_at,
    /** True only when Developer Account is approved AND enrolled in Developer Beta. */
    developerEnrolled: isDeveloperAccount && enrollmentStatus === "approved",
  };
}

function requireAdmin(env: Env, request: Request): Response | null {
  const expected = env.ADMIN_TOKEN;
  if (!expected) {
    return error("Admin not configured (set ADMIN_TOKEN secret)", 503);
  }
  const got = request.headers.get("x-admin-token") || "";
  if (got !== expected) {
    return error("Unauthorized", 401);
  }
  return null;
}

async function handleRegister(env: Env, request: Request): Promise<Response> {
  const body = await readJson<{
    email?: string;
    password?: string;
    displayName?: string;
  }>(request);
  if (!body) return error("Invalid JSON");

  const email = normalizeEmail(body.email || "");
  const password = body.password || "";
  const displayName = (body.displayName || "").trim().slice(0, 64);

  if (!isValidEmail(email)) return error("Invalid email");
  if (password.length < MIN_PASSWORD) {
    return error(`Password must be at least ${MIN_PASSWORD} characters`);
  }

  const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?")
    .bind(email)
    .first();
  if (existing) return error("Email already registered", 409);

  const id = randomId();
  const passwordHash = await hashPassword(password);
  const createdAt = nowIso();

  await env.DB.batch([
    env.DB.prepare(
      `INSERT INTO users
        (id, email, password_hash, display_name, created_at, developer_status, developer_requested_at, developer_reviewed_at)
       VALUES (?, ?, ?, ?, ?, 'none', NULL, NULL)`
    ).bind(id, email, passwordHash, displayName, createdAt),
    env.DB.prepare(
      "INSERT INTO enrollments (user_id, status, requested_at, reviewed_at, note) VALUES (?, 'none', NULL, NULL, '')"
    ).bind(id),
  ]);

  const token = await createSession(env, id);
  const user: UserRow = {
    id,
    email,
    password_hash: passwordHash,
    display_name: displayName,
    created_at: createdAt,
    developer_status: "none",
  };
  const enrollment = await getEnrollment(env, id);
  return json({ token, user: mePayload(user, enrollment) }, 201);
}

async function handleLogin(env: Env, request: Request): Promise<Response> {
  const body = await readJson<{ email?: string; password?: string }>(request);
  if (!body) return error("Invalid JSON");

  const email = normalizeEmail(body.email || "");
  const password = body.password || "";
  if (!email || !password) return error("Email and password required");

  const user = await env.DB.prepare(
    `SELECT ${USER_SELECT} FROM users WHERE email = ?`
  )
    .bind(email)
    .first<UserRow>();
  if (!user) return error("Invalid email or password", 401);

  const ok = await verifyPassword(password, user.password_hash);
  if (!ok) return error("Invalid email or password", 401);

  const token = await createSession(env, user.id);
  const enrollment = await getEnrollment(env, user.id);
  return json({ token, user: mePayload(user, enrollment) });
}

async function handleLogout(env: Env, request: Request): Promise<Response> {
  const token = bearerToken(request);
  if (token) {
    const tokenHash = await sha256Hex(token);
    await env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?")
      .bind(tokenHash)
      .run();
  }
  return json({ ok: true });
}

async function handleMe(env: Env, request: Request): Promise<Response> {
  const user = await userFromRequest(env, request);
  if (!user) return error("Unauthorized", 401);
  const enrollment = await getEnrollment(env, user.id);
  return json(mePayload(user, enrollment));
}

/** Step 1 — apply for a Developer Account (admin must approve). */
async function handleDeveloperAccountRequest(env: Env, request: Request): Promise<Response> {
  const user = await userFromRequest(env, request);
  if (!user) return error("Unauthorized", 401);

  const status = developerStatusOf(user);
  if (status === "approved" || status === "pending") {
    const enrollment = await getEnrollment(env, user.id);
    return json(mePayload(user, enrollment));
  }

  const requestedAt = nowIso();
  await env.DB.prepare(
    `UPDATE users SET
       developer_status = 'pending',
       developer_requested_at = ?,
       developer_reviewed_at = NULL
     WHERE id = ?`
  )
    .bind(requestedAt, user.id)
    .run();

  const refreshed = await env.DB.prepare(`SELECT ${USER_SELECT} FROM users WHERE id = ?`)
    .bind(user.id)
    .first<UserRow>();
  const enrollment = await getEnrollment(env, user.id);
  return json(mePayload(refreshed || user, enrollment));
}

/**
 * Step 2 — enroll in Developer Beta.
 * Requires an approved Developer Account; enrollment is immediate (no second admin step).
 */
async function handleEnrollRequest(env: Env, request: Request): Promise<Response> {
  const user = await userFromRequest(env, request);
  if (!user) return error("Unauthorized", 401);

  if (developerStatusOf(user) !== "approved") {
    return error("A Developer Account is required before you can enroll", 403);
  }

  const enrollment = await getEnrollment(env, user.id);
  if (enrollment.status === "approved") {
    return json(mePayload(user, enrollment));
  }

  const requestedAt = nowIso();
  await env.DB.prepare(
    `INSERT INTO enrollments (user_id, status, requested_at, reviewed_at, note)
     VALUES (?, 'approved', ?, ?, '')
     ON CONFLICT(user_id) DO UPDATE SET
       status = 'approved',
       requested_at = excluded.requested_at,
       reviewed_at = excluded.reviewed_at,
       note = ''`
  )
    .bind(user.id, requestedAt, requestedAt)
    .run();

  const updated = await getEnrollment(env, user.id);
  return json(mePayload(user, updated));
}

async function handleAdminList(env: Env, request: Request, url: URL): Promise<Response> {
  const denied = requireAdmin(env, request);
  if (denied) return denied;

  const status = url.searchParams.get("status") || "pending";
  const rows = await env.DB.prepare(
    `SELECT id as user_id, email, display_name, created_at,
            developer_status as status,
            developer_requested_at as requested_at,
            developer_reviewed_at as reviewed_at
     FROM users
     WHERE developer_status = ?
     ORDER BY developer_requested_at DESC`
  )
    .bind(status)
    .all();

  return json({
    items: (rows.results || []).map((r) => ({
      userId: r.user_id,
      email: r.email,
      displayName: r.display_name,
      status: r.status,
      requestedAt: r.requested_at,
      reviewedAt: r.reviewed_at,
      createdAt: r.created_at,
      kind: "developer_account",
    })),
  });
}

async function handleAdminDecision(
  env: Env,
  request: Request,
  userId: string,
  decision: "approved" | "denied"
): Promise<Response> {
  const denied = requireAdmin(env, request);
  if (denied) return denied;

  const user = await env.DB.prepare("SELECT id FROM users WHERE id = ?")
    .bind(userId)
    .first();
  if (!user) return error("User not found", 404);

  const reviewedAt = nowIso();
  await env.DB.prepare(
    `UPDATE users SET
       developer_status = ?,
       developer_reviewed_at = ?
     WHERE id = ?`
  )
    .bind(decision, reviewedAt, userId)
    .run();

  // If Developer Account is denied/revoked, drop beta enrollment.
  if (decision === "denied") {
    await env.DB.prepare(
      `UPDATE enrollments SET status = 'none', requested_at = NULL, reviewed_at = NULL
       WHERE user_id = ?`
    )
      .bind(userId)
      .run();
  }

  return json({ ok: true, userId, status: decision, kind: "developer_account" });
}

function adminHtml(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Polar — Developer Accounts</title>
  <style>
    :root { color-scheme: dark; font-family: system-ui, sans-serif; }
    body { margin: 0; background: #0b0d12; color: #e8eaef; }
    main { max-width: 720px; margin: 0 auto; padding: 24px 16px 48px; }
    h1 { font-size: 1.4rem; margin: 0 0 8px; }
    p { color: #9aa3b2; }
    input, button { font: inherit; }
    input { width: 100%; box-sizing: border-box; padding: 10px 12px; border-radius: 10px;
      border: 1px solid #2a3140; background: #151922; color: inherit; margin: 8px 0 12px; }
    button { padding: 8px 14px; border-radius: 999px; border: 0; cursor: pointer;
      background: #0a84ff; color: #fff; font-weight: 600; }
    button.ghost { background: #2a3140; }
    button.danger { background: #b42318; }
    .row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
    .card { background: #151922; border: 1px solid #222836; border-radius: 14px;
      padding: 14px 16px; margin: 10px 0; }
    .muted { color: #9aa3b2; font-size: 0.9rem; }
    .err { color: #f87171; }
    .ok { color: #34d399; }
  </style>
</head>
<body>
  <main>
    <h1>Developer Accounts</h1>
    <p>Approve who can become a Polar Developer Account. Once approved, they can enroll in Developer Beta themselves.</p>
    <label>Admin token</label>
    <input id="token" type="password" placeholder="ADMIN_TOKEN" autocomplete="off" />
    <div class="row">
      <button id="load">Load pending</button>
      <button class="ghost" id="loadApproved">Approved</button>
      <button class="ghost" id="loadDenied">Denied</button>
    </div>
    <p id="msg" class="muted"></p>
    <div id="list"></div>
  </main>
  <script>
    const msg = document.getElementById('msg');
    const list = document.getElementById('list');
    const tokenEl = document.getElementById('token');
    tokenEl.value = localStorage.getItem('polar_admin_token') || '';

    function token() {
      const t = tokenEl.value.trim();
      localStorage.setItem('polar_admin_token', t);
      return t;
    }

    async function load(status) {
      msg.textContent = 'Loading…';
      list.innerHTML = '';
      try {
        const res = await fetch('/v1/admin/developer-accounts?status=' + encodeURIComponent(status), {
          headers: { 'X-Admin-Token': token() }
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || res.statusText);
        const items = data.items || [];
        msg.textContent = items.length + ' ' + status;
        msg.className = 'ok';
        if (!items.length) {
          list.innerHTML = '<p class="muted">No items.</p>';
          return;
        }
        for (const item of items) {
          const card = document.createElement('div');
          card.className = 'card';
          card.innerHTML =
            '<strong>' + (item.displayName || item.email) + '</strong>' +
            '<div class="muted">' + item.email + '</div>' +
            '<div class="muted">Requested: ' + (item.requestedAt || '—') + '</div>' +
            '<div class="row" style="margin-top:10px"></div>';
          const row = card.querySelector('.row');
          if (status === 'pending' || status === 'denied') {
            const a = document.createElement('button');
            a.textContent = 'Approve';
            a.onclick = () => decide(item.userId, 'approve');
            row.appendChild(a);
          }
          if (status === 'pending' || status === 'approved') {
            const d = document.createElement('button');
            d.className = 'danger';
            d.textContent = 'Deny';
            d.onclick = () => decide(item.userId, 'deny');
            row.appendChild(d);
          }
          list.appendChild(card);
        }
      } catch (e) {
        msg.textContent = e.message || String(e);
        msg.className = 'err';
      }
    }

    async function decide(userId, action) {
      msg.textContent = action + '…';
      const res = await fetch('/v1/admin/developer-accounts/' + userId + '/' + action, {
        method: 'POST',
        headers: { 'X-Admin-Token': token() }
      });
      const data = await res.json();
      if (!res.ok) {
        msg.textContent = data.error || res.statusText;
        msg.className = 'err';
        return;
      }
      await load('pending');
    }

    document.getElementById('load').onclick = () => load('pending');
    document.getElementById('loadApproved').onclick = () => load('approved');
    document.getElementById('loadDenied').onclick = () => load('denied');
  </script>
</body>
</html>`;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";

    try {
      if (request.method === "GET" && (path === "/admin" || path === "/")) {
        if (path === "/") {
          return json({
            service: "polar-accounts",
            ok: true,
            admin: "/admin",
          });
        }
        return new Response(adminHtml(), {
          headers: { "content-type": "text/html; charset=utf-8", ...corsHeaders() },
        });
      }

      if (request.method === "POST" && path === "/v1/auth/register") {
        return await handleRegister(env, request);
      }
      if (request.method === "POST" && path === "/v1/auth/login") {
        return await handleLogin(env, request);
      }
      if (request.method === "POST" && path === "/v1/auth/logout") {
        return await handleLogout(env, request);
      }
      if (request.method === "GET" && path === "/v1/me") {
        return await handleMe(env, request);
      }
      if (request.method === "POST" && path === "/v1/developer/request") {
        return await handleDeveloperAccountRequest(env, request);
      }
      if (request.method === "POST" && path === "/v1/enroll/request") {
        return await handleEnrollRequest(env, request);
      }
      if (request.method === "GET" && path === "/v1/enroll/status") {
        return await handleMe(env, request);
      }

      // Admin: Developer Accounts (primary) + legacy enrollments alias
      if (
        request.method === "GET" &&
        (path === "/v1/admin/developer-accounts" || path === "/v1/admin/enrollments")
      ) {
        return await handleAdminList(env, request, url);
      }

      const approveDev = /^\/v1\/admin\/(?:developer-accounts|enrollments)\/([^/]+)\/approve$/.exec(path);
      if (request.method === "POST" && approveDev) {
        return await handleAdminDecision(env, request, approveDev[1], "approved");
      }
      const denyDev = /^\/v1\/admin\/(?:developer-accounts|enrollments)\/([^/]+)\/deny$/.exec(path);
      if (request.method === "POST" && denyDev) {
        return await handleAdminDecision(env, request, denyDev[1], "denied");
      }

      return error("Not found", 404);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Server error";
      return error(message, 500);
    }
  },
};
