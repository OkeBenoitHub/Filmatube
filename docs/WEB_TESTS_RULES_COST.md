# Web Tests, Firestore Rules Final Audit & Cost Review (Day 122)

## Web tests + CI gating (applied)
- **Vitest** added (`npm test` → `vitest run`) with a `server-only` stub so pure logic in server
  modules is unit-testable. Path alias `@` and `test/**/*.test.ts` include configured.
- **10 tests** covering `tasteMatch` (Jaccard %) and the catalog selectors `localized`,
  `searchMovies`, `pickFeatured`, `pickComingSoon`.
- **CI:** `web.yml` now runs `npm test` between lint and build → tests gate every push/PR.
- **E2E (follow-up):** a Playwright smoke suite (landing renders, login form, protected-route
  redirect) is the next step; it needs browsers in CI (`npx playwright install --with-deps`) and is
  deferred so CI stays fast. Documented here as the remaining test-pyramid tier.

## Firestore rules — final audit
Re-reviewed the full ruleset (consolidates Day 117 + Day 121). All collections gated:

| Area | Rule |
|---|---|
| `users/{uid}` | read: signed-in; write: self (can't change `isAdmin`/`isBanned`) or admin |
| `users/{uid}/{notifications,fcmTokens,settings,profiles}` | self; notifications create requires `actorId==auth.uid && type!='system'` |
| `movies` | read published (or admin); write admin |
| `watchProgress`/`watchlists`/`likes`/`reactions`/`ratings` | self-write, signed-in read |
| `reviews`/`comments` (+ `/likes`) | create/update require `userId==auth.uid`; delete owner/admin |
| `collections` (+ `/items`) | owner or public read; owner/admin write |
| `follows` | create requires `followerId==auth.uid` + id shape; delete owner |
| `feed` | recipient read; create requires `actorId==auth.uid` |
| `recommendations` | recipient read; create requires `fromUserId==auth.uid` |
| `reports` | create requires `reporterId==auth.uid`; read/resolve admin |
| `broadcasts` | admin only |
| `rateLimits` | **deny-all** (admin SDK only) |

**No open rules.** The `admin` custom claim is authoritative. Carried-forward hardening (tracked, not
blockers): split `users/{uid}.email` into a private subtree; enforce `isBanned` via a custom claim.

## Cost review (estimate at beta scale — Blaze)
| Service | Driver | Notes |
|---|---|---|
| **Firestore** | reads dominate (home rows, feed, reviews) | Reads are capped (`limit`) and index-backed; realtime listeners bounded. Biggest lever: derive follower counts via a Function (avoids per-view `follows` scans) at scale. |
| **Cloud Functions** | `aggregateRatings` (per rating write) + `processScheduledBroadcasts` (every 5 min) | Low volume; within free tier for beta. The scheduler runs 288×/day regardless — trivial. |
| **Cloud Messaging** | free | No per-message cost. |
| **R2 (Cloudflare)** | storage + egress | **Egress is free** on R2 — the main win vs S3; video storage is the cost. Class-A/B ops modest. |
| **Hosting** | static + SSR | Next SSR on the chosen host (Firebase App Hosting / Vercel). Landing/legal can be static-rendered to cut SSR invocations. |
| **Auth / App Check** | free tiers | Play Integrity + (future) reCAPTCHA within free limits. |

**Watch items:** (1) unbounded `follows` reads for counts → Function-maintained counters; (2) Firestore
read amplification on hot movies (reviews/comments) — already capped at 100/200; (3) keep an eye on
Functions invocations if fan-out moves server-side.

## Status
Vitest + CI gating live (10 tests). Rules re-audited — no open rules. Cost profile is beta-safe;
R2 zero-egress is the key economics. Playwright E2E is the one deferred item.
