# v1.3 hardening + Crashlytics review (Day 208)

A pre-release pass over the v1.3 systems (recs, referral, gamification): close the last verification
gap, audit the new server code and rules, and confirm the client crash surface.

## 1. Referral flow — verified end to end (was the top open item)
Previously build-only. `scripts/seed-referrals-demo.mjs` writes `referrals/{}` docs exactly as the
session route does, firing the **deployed** `onReferralCreated` trigger against prod:

```
3 referrals → referralCount 0 → 3 · earlyAccess true · Recruiter badge granted · 3 notifications
Fraud guard: 2 of 3 flagged "shared signup IP" (they share an ipHash)
```

The reward chain (count, entitlement, badge, inbox+FCM) works. The two attribution *entry points*
(web session cookie, Android `/api/referral` bearer) remain untested by a real browser/device
sign-up — but both funnel into this same verified `recordReferral` → trigger path, so the
unverified part is now just the cookie/deep-link plumbing, not the reward logic.

## 2. Cloud Functions robustness (all `europe-west4`)
Reviewed the six v1.3 functions:
- **Bounded.** Every batch caps users at `limit(500)` and wraps each user in its own `try/catch`,
  so one bad document can't fail the whole run.
- **Idempotent.** Badges use fixed doc ids; the streak is guarded by `streakLastDay`; referral
  attribution is keyed by the referred user with an `exists` check. Re-runs don't double-count —
  proven for gamification (a second run awarded 0) and for referral (the `exists` guard).
- **Aggregation reads** (`count()`) for watch/follow totals instead of pulling docs.
- **FCM failures are swallowed** in `notifyUser` after the inbox doc is written, so a bad token
  can't break awarding.
- **Scale note (not a bug):** `buildStats` reads *all* of a user's `watchProgress` items with no
  page limit. Bounded by the catalogue today (~movies watched); revisit with pagination if a single
  user's item count ever grows large.

## 3. Rules audit — v1.3 collections
| Collection | Read | Write |
|---|---|---|
| `recs/{uid}` | self | **false** (function) |
| `recFeedback/{uid}/items` | self | self |
| `homeRows` | signed-in | admin |
| `referrals/{referredId}` | admin · self · referrer | **false** (function) |
| `achievements/{uid}/badges` | signed-in | **false** (function) |
| `stats/{uid}` | signed-in | **false** (function) |

Everything that must be unforgeable (recs, referrals, badges, stats) is **function-write-only**;
clients can never self-award or fabricate a referral. Signed-in read on badges/stats is intentional
(they show on public profiles).

## 4. Client crash surface
- **Android.** The new repositories (`ReferralRepository`, `AchievementsRepository`,
  `StatsRepository`) use `runCatching` / callback-flow with null-safe reads; `attributePendingInvite`
  is best-effort and clears the pending code regardless. `MainActivity.captureInvite` guards every
  field of the deep-link URI. No new uncaught paths. `CrashReporter` (Day 115) remains wired at the
  player; the v1.3 screens are read-only over Firestore flows, a low crash surface.
- **Web.** v1.3 pages are server components over the admin SDK (fail closed to empty state) or
  client components with `try/catch` around network/clipboard/share. `next/og` routes render even
  when the user or stats doc is missing (fall back to defaults), so a bad uid returns an image, not
  a 500.

## 5. Fixes applied this pass
- **Stats-card caching.** `/api/stats-card/{uid}` now sends `Cache-Control: public, max-age=3600,
  s-maxage=86400`. The route is public and CPU-bound (image render); without caching, a shared card
  unfurled by many clients would re-render every hit. Stats change at most daily, so an hour of
  client cache / a day at the edge is safe.

## Residual risks accepted for v1.3
- **Stats card is enumerable by uid** and unauthenticated — by design (it must be fetchable for
  social unfurls). It exposes only aggregates already public on `/u/{id}`.
- **Referral entry points** (cookie / deep link) still want one real end-to-end sign-up on each
  platform before wide release — carried into beta (Day 209).
- **Badge lag** up to 24h (only Recruiter is immediate) — acceptable for launch; per-signal
  triggers are a fast-follow.

## Build status
Web: `tsc` + `next lint` + **22 Vitest tests** green. Android: `:app:assembleDebug` green. All six
v1.3 functions deployed and confirmed via `firebase functions:list`.
