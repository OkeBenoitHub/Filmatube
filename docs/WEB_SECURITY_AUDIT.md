# Web Security Audit (Day 121)

## Security headers (applied — `next.config.mjs`)
Every response now carries:
- `X-Frame-Options: DENY` — anti-clickjacking (the app embeds no iframes).
- `X-Content-Type-Options: nosniff`.
- `Referrer-Policy: strict-origin-when-cross-origin`.
- `Permissions-Policy: camera=(), microphone=(), geolocation=()` — deny sensitive APIs.
- `Strict-Transport-Security: max-age=63072000; includeSubDomains; preload` (HTTPS only).
- CSP intentionally **not** enforced yet — Firebase JS + inline needs a tuned policy; add as
  `Content-Security-Policy-Report-Only` first, then enforce. Tracked follow-up.

## Session hardening (verified)
- Auth uses an **httpOnly** session cookie (`__session`): `secure` in production, `sameSite: lax`,
  `path: /`, 5-day expiry.
- `getCurrentUser()` calls `verifySessionCookie(cookie, true)` — the `true` **checks revocation**, so
  a signed-out/disabled session is rejected server-side.
- `/admin/*` is gated by middleware (cookie presence) **and** `requireAdmin` (admin claim) in the
  layout; the admin claim (not a users-doc field) is authoritative.
- Server Actions and admin API routes re-assert the admin claim (`user.admin === true`) independently.

## Presign / upload hardening (applied)
- `/api/uploads/presign` requires a valid session **and** now enforces a **per-user rate limit**
  (30 requests/min, `lib/rate-limit.allowRequest` — Firestore fixed-window, fails open).
- Bucket authorization: `videos`/`images`/`subtitles` are admin-only; `avatars` are scoped to
  `avatars/{uid}/…`; filenames are sanitized; keys use a random UUID.
- New `rateLimits/*` collection is **deny-all** in rules (written only by the admin SDK). Deployed.

## R2 anti-hotlink / expiry (audit)
- **`videos`** bucket is **private** — served only via short-lived presigned URLs (playback ~1h).
  No public access → no hotlinking possible.
- **`images`/`avatars`/`subtitles`** are public-read (posters/avatars must load in `<img>`). Presigned
  **uploads** expire quickly; public **reads** are cacheable by design.
- **Recommended (Cloudflare console):** for the public buckets, add a **hotlink-protection / Referer
  rule** at the R2/Cloudflare edge so assets only load from the app's own domains, and set a cache
  rule. This is an edge-config action (not app code).

## App Check (web)
- Not yet wired on the web client. Server routes are already protected by the session cookie + admin
  claim, so this is defense-in-depth. To add: initialize `firebase/app-check` with
  **reCAPTCHA Enterprise** using `NEXT_PUBLIC_RECAPTCHA_SITE_KEY`, then enable App Check enforcement
  in the console once traffic looks healthy. (User action: create the reCAPTCHA key.)

## Status
Headers + presign rate-limiting applied and building green; `rateLimits` locked down + rules
deployed. Session/admin gating verified. Remaining: CSP (report-only → enforce), web App Check
(reCAPTCHA key), and the Cloudflare hotlink rule for public buckets.
