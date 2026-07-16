# Day 152 — Hardening + Crashlytics review

Pre-v1.1 audit of the security rules, PII surface and crash reporting.
Project **filmatubelive** (Firestore in **eur3**).

## Fixed in this pass

### users/{uid} leaked every account's email address — FIXED
`users/{uid}` is readable by any signed-in user (`allow read: if isSignedIn()`), and it must be:
display names and avatars are joined from it all over the app (boards, parties, comments,
notifications, member lists). It also stored `email`.

That field was **written by both clients and read by nothing** — a grep of both codebases found
zero consumers (the Android hits were the forgot-password form's own local state). So every
signed-in account could read every other account's email for no product benefit.

- Both clients no longer write it (`lib/user.ts`, `UserRepositoryImpl.kt`), with a comment
  explaining why so it doesn't creep back.
- The field was deleted from the 5 existing user docs (verified: 0 remain).
- **Firebase Auth remains the authoritative store** — sign-in, password reset and recovery are
  unaffected. Read it server-side from Auth if a feature ever genuinely needs it.

This closes the "split users.email → private subtree" item from the Day 126 launch checklist,
and more cheaply: the address simply isn't duplicated into Firestore at all.

## Verified healthy
- **Crashlytics** is wired: Gradle plugin + dependency, and `CrashReporter` exposes
  `recordException` / `setCustomKey`. Non-fatals are already recorded on real failure paths
  (e.g. player load). Release builds are minified with R8 and upload mapping files.
- **`isAdmin()` uses a custom claim** (`request.auth.token.admin`), not a Firestore field — a
  user cannot escalate by writing their own doc.
- **Self-escalation is blocked**: `users` create/update assert `isAdmin`/`isBanned` are unchanged.
- **`rateLimits/{id}` is deny-all** to clients (server-only).
- **Field-scoped writes** are used where clients need partial access: membership-only board/party
  updates (`hasOnly(['memberIds','memberCount'])` toggling only your own uid) and reactions-only
  message updates. The party `sync/state` doc is **host-only write** — the sync engine's
  authority is enforced by rules, not just UI.
- **Private-by-default reads**: parties are host-or-member only; private boards likewise.
- Unmatched paths fall through to Firestore's default deny.

## Open — must decide before launch

### 1. Banned users are not actually blocked (HIGH)
`isBanned` is only used to stop self-escalation and to filter suggestion lists / broadcast
recipients. **A banned user can still sign in and use everything.** There is also no ban UI —
`/admin/users` is still a placeholder.

The clean fix mirrors `isAdmin`: a **`banned` custom claim** checked in `isSignedIn()` (or a
`isNotBanned()` helper) so rules reject every write, plus an admin action that sets the claim and
revokes refresh tokens. Needs an Admin-SDK path; not a rules-only change.

### 2. Session revocation lags up to 5 days (ACCEPTED)
`getCurrentUser` verifies the session cookie **locally** (no `checkRevoked`) — a deliberate
trade made to fix navigation latency, documented in `lib/auth/session.ts`. Sign-out clears the
cookie, so this only matters for server-side revocation (i.e. banning). Revisit together with #1.

### 3. Not yet done from the Day 126 checklist
- Web **App Check** (reCAPTCHA) — Android already has App Check.
- **CSP** report-only → enforce (no `Content-Security-Policy` header is set today; the security
  headers in `next.config.mjs` cover X-Frame-Options, nosniff, Referrer-Policy, HSTS,
  Permissions-Policy).
- Cloudflare hotlink protection on the R2 public buckets.
- The `firestore-send-email` extension (v1 trigger, us-central1) was bound to the **old nam5**
  database and is almost certainly broken post-migration. Nothing in Filmatube appears to use it —
  uninstall or reconfigure.

### 4. Housekeeping that will bite later
- `parties/{id}/reactions` and `/messages` accumulate forever; ended parties are never deleted.
  A scheduled Function or TTL policy should sweep them.
- No automatic host-drop detection in parties (see `PARTIES_QA.md`).

## Status
Both builds green after the change. The email leak is closed for existing **and** new accounts.
The ban gap (#1) is the one item I would not launch without.
