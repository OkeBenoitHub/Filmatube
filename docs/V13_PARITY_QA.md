# v1.3 cross-platform parity QA — recs · referral · gamification (Day 207)

Confirms the three v1.3 systems behave the same on Android and web, and records where they
deliberately differ. Companion to `RECS_QA.md` (algorithm) and `GROWTH_GAMIFICATION_QA.md`
(referral/gamification detail + live-data verification).

## Shared backend = parity by construction
Both clients read the same Firestore documents, written by the same Cloud Functions. There is no
per-platform scoring or awarding logic to drift:

| System | Shared source of truth | Functions |
|---|---|---|
| Recommendations | `recs/{uid}` `{topPicks, rows}`, `recFeedback/{uid}/items` | `buildRecommendations` (`recs-core.js`) |
| Referral | `referrals/{referredId}`, `users.{referralCount,earlyAccess}` | `onReferralCreated` |
| Gamification | `achievements/{uid}/badges`, `stats/{uid}` | `awardBadges`, `buildStats`, `onReviewWritten`, `onPremiereAttended` (`gamification-core.js`) |

## Feature parity

| Capability | Web | Android | Notes |
|---|---|---|---|
| "Top picks for you" | ✓ | ✓ | same `recs.topPicks` |
| "Because you watched X" rails | ✓ | ✓ | same `recs.rows`, resolved against the loaded catalogue |
| "From people you follow" | ✓ | ✓ | both aggregate the viewer's own feed by movie |
| "Not interested" → `recFeedback` | ✓ (movie menu) | ✓ (option sheet) | rec rows only |
| Invite link `/invite/{uid}` | ✓ | ✓ | Android opens it via `filmatube://invite/{code}` |
| Referral attribution on first signup | ✓ session route | ✓ `POST /api/referral` (bearer) | **different path, same result** — see below |
| Referral dashboard / status | `/refer` | `ReferralScreen` | link + share + friends-joined list |
| Reward: Recruiter badge + earlyAccess + FCM | ✓ | ✓ | server-side, platform-independent |
| Badges (earned vs locked) | account + `/u/{id}` | profile | same 7 ids/emoji; names in each i18n |
| Stats tiles (hours / movies / reviews) | account + `/u/{id}` | profile | |
| Streak + weekly goal | ✓ | ✓ | |
| Shareable stats card | ✓ PNG (`/api/stats-card`) | ✗ | **web-only** — see differences |
| Admin referral analytics + revoke | ✓ `/admin/referrals` | n/a | admin is web-only by design |

## Deliberate differences
1. **Referral attribution path.** Web attributes inside `/api/auth/session` (reads the
   `filmatube.ref` cookie); Android has no web session, so it captures the code from a
   `filmatube://invite/{code}` deep link into a pref and calls `POST /api/referral` with the
   Firebase id-token after sign-up. Both end in the same server-only `recordReferral`, so the
   `referrals/{}` doc and reward are identical. *This is the one genuine mechanism difference.*
2. **Shareable stats card is web-only.** It's rendered by `next/og` (a Node/Edge image route);
   there's no Android equivalent yet. Android's referral/stats screens share text + link, not an
   image. A future Android card would render a Compose bitmap and share it.
3. **Admin surfaces (curation, editorial, referral analytics) are web-only** — consistent with the
   whole admin area.

## Integrity (same on both platforms, because it's server-side)
- Referrals are **unforgeable**: rules deny all client writes; only the trusted server path
  creates them. Self-referral and repeat-referral are rejected in `recordReferral`.
- Badges/stats are **function-written**, signed-in-read; clients cannot self-award.
- Fraud: hashed signup-IP clustering flags multi-account farming for admin review; revoke rolls
  back the reward. (Day 206.)

## Verification status (honest)
- **Gamification (badges + stats): verified against live data.** `run-gamification.mjs` (same
  `gamification-core.js` the functions run) populated both demo accounts and awarded First Watch;
  a second run proved idempotency. See `GROWTH_GAMIFICATION_QA.md`.
- **Recs: seeded and rendering.** `seed-recs-demo.mjs` built rec docs for both accounts; rails
  resolve. Not yet eyeballed on a real device beyond the seed.
- **Referral: NOT exercised end to end.** No real invited signup has run on either platform — the
  single biggest open item for v1.3. The fraud classifier is unit-tested (5 tests); the attribution
  wiring is build-verified only.
- **Fraud classifier: unit-tested** (`test/referral-fraud.test.ts`, part of 22 passing tests).

## Builds
Web: `tsc` + `next lint` + **22 Vitest tests** green. Android: `:app:assembleDebug` green.

## Follow-ups carried into hardening (Days 208+)
- Run the referral flow end to end (web + Android) — the one unverified path.
- Immediate badge awards (per-signal triggers) vs the current up-to-24h nightly lag.
- Consume `earlyAccess` in the theater; let users set `weeklyGoal`; consider an Android stats card.
