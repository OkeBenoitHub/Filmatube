# v1.3 release readiness (Days 209–210)

Beta/fix pass and the go-live checklist for v1.3 (Discovery & Growth: recs, referral, achievements,
stats, gamification).

## Pre-flight — done
- [x] **Production build passes.** `NEXT_DIST_DIR=.next-verify next build` compiled clean (exit 0),
      all routes including the new `/invite/[code]`, `/refer`, `/admin/referrals`, `/library/*`.
      This catches server-component/build errors that `tsc` + `lint` do not.
- [x] **Web tests green** — 22 Vitest (recs scorer, fraud classifier, catalog/social selectors).
- [x] **Android builds** — `:app:assembleDebug`.
- [x] **All v1.3 Cloud Functions deployed** (europe-west4): `buildRecommendations`,
      `onReferralCreated`, `awardBadges`, `buildStats`, `onReviewWritten`, `onPremiereAttended`.
- [x] **Rules deployed** — recs / recFeedback / homeRows / referrals / achievements / stats.
- [x] **Reward chains verified against live data** — gamification (`run-gamification.mjs`) and the
      referral trigger (`seed-referrals-demo.mjs`).

## Deploy target
- Backend: Firebase App Hosting backend `filmatube`, project `filmatubelive`, region europe-west4.
- Live URL: `https://filmatube--filmatubelive.europe-west4.hosted.app` (no custom domain yet).
- Account for CLI deploys: `filmatube.app.base@gmail.com`.
- Release branch: currently **56 commits ahead of `main`** on `feat/online-movie-theater`.

## Go-live steps (Day 210)
1. Verify identity: `firebase projects:list` shows `filmatubelive`; deploying as the Filmatube
   account (not a sibling app's).
2. Land the code on the release branch (merge `feat/online-movie-theater` → `main`, or deploy
   local-source from the feature branch — **the one open decision, deferred to the user**).
3. `firebase deploy --only apphosting --project filmatubelive` (or push to the connected branch).
   Watch for the `EntityTooLarge` / ignore gotcha in the deployment runbook.
4. Smoke test the live URL (see below).
5. Confirm monitoring is receiving data.

## Smoke test (post-deploy)
- [ ] `/` landing loads; sign in.
- [ ] `/home` shows rec rails on a seeded account; "Not interested" works.
- [ ] `/account` shows stats tiles, streak, weekly goal, badge grid, and the stats card renders.
- [ ] `/refer` shows the invite link + friends-joined; `/invite/<uid>` landing loads logged-out.
- [ ] `/admin/referrals` (as admin) shows analytics + the flagged demo cluster.
- [ ] `/api/stats-card/<uid>` returns a PNG with `Cache-Control`.

## Monitoring
- **Functions:** Cloud Functions logs + the scheduled-job console; each nightly job logs a summary
  line (`recs: built …`, `stats: built …`, `badges: awarded …`). Errors are per-user `console.error`
  so one failure doesn't hide the rest.
- **Android:** Crashlytics + Performance already wired (Days 114–115); `CrashReporter` records
  non-fatals at the player.
- **Web:** App Hosting request/error metrics; Firebase Analytics events already logged for playback.

## Beta fixes applied
- Stats-card caching (`Cache-Control`) — see `V13_HARDENING.md`.

## Deliberate follow-ups (post-launch, not blockers)
- **Referral entry points** — one real signup-through-invite on web and Android (reward path already
  verified; only the cookie/deep-link capture is unproven).
- **`REFERRAL_IP_SALT`** — a real salt secret would strengthen the fraud-hash; the default fallback
  works (the hash is still a per-network clustering signal). Set via
  `firebase apphosting:secrets:set REFERRAL_IP_SALT` + add to `apphosting.yaml` when desired.
- **Immediate badges** (per-signal triggers) vs the 24h nightly lag.
- **Consume `earlyAccess`** in the theater; user-settable `weeklyGoal`; Android stats card.
- **Demo data** seeded in prod (30-title catalogue, rec docs, 2 editorial + 2 home rows, 3 referral
  demo accounts under BENOIT) — clear from the Firebase console before or shortly after public launch.
