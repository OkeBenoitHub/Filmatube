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

### Banned users could still use the app — FIXED
`isBanned` was only used to stop self-escalation and to filter suggestion lists / broadcast
recipients; a banned user could still sign in and do everything, and there was no ban UI.

Now enforced the same way `admin` is — via a **custom claim**, never a doc field (a user can
write their own doc, so a doc flag would be self-serve):

- `isSignedIn()` returns false for a banned claim. Every other rule builds on it — `isSelf`,
  `isAdmin`, and all 67 call sites — so **one helper denies a banned account every read and
  write** in the ruleset. Deployed.
- `/admin/users` is now a real console (it was a placeholder): ban/unban, admin badges, and it
  reads ban state from the **claim**, flagging any user whose doc mirror disagrees.
- `setUserBanned` preserves other claims (`setCustomUserClaims` replaces the whole object, so a
  naive write would strip `admin`), then **revokes refresh tokens** — that's what makes it bite:
  the client must mint a fresh ID token, which carries the claim, so Firestore starts rejecting
  it (immediately on re-auth, otherwise within the hourly refresh). Admins can't ban themselves.
- `users/{uid}.isBanned` is still mirrored for the existing filters, but is **not** the source
  of truth.
- `getCurrentUser` treats a banned claim as signed out.

**Residual gap, stated honestly:** claims are baked into the web session cookie when it's minted,
so banning someone *mid-session* leaves that cookie technically valid until it expires (≤5 days).
They cannot mint a new one (tokens revoked) and Firestore rejects all their data access, so the
pages they can still reach are empty shells. Fully closing it needs
`verifySessionCookie(cookie, true)` — a network round-trip per navigation, which is exactly the
latency we removed. Revisit if a real abuse case demands it.

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

### 1. Not yet done from the Day 126 checklist
- Web **App Check** (reCAPTCHA) — Android already has App Check.
- **CSP** report-only → enforce (no `Content-Security-Policy` header is set today; the security
  headers in `next.config.mjs` cover X-Frame-Options, nosniff, Referrer-Policy, HSTS,
  Permissions-Policy).
- Cloudflare hotlink protection on the R2 public buckets.
- The `firestore-send-email` extension (v1 trigger, us-central1) was bound to the **old nam5**
  database and is almost certainly broken post-migration. Nothing in Filmatube appears to use it —
  uninstall or reconfigure.

### 2. Housekeeping that will bite later
- `parties/{id}/reactions` and `/messages` accumulate forever; ended parties are never deleted.
  A scheduled Function or TTL policy should sweep them.
- No automatic host-drop detection in parties (see `PARTIES_QA.md`).

## Status
Both builds green. The two findings from this audit — the email leak and unenforced bans — are
both closed and deployed. What remains open is the Day 126 launch checklist (App Check, CSP,
hotlink protection, the stale email extension) and housekeeping, none of which blocks a launch
the way the ban gap did.
