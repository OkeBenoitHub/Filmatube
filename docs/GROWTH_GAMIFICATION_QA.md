# Referral, achievements & stats — QA and Week 29 review

Covers Days 197–203 (v1.3 growth + gamification): the referral loop, the achievement engine,
the stats roll-up, and light gamification.

## What was built

### Referral (197–199)
- **Invite link** `/invite/{uid}` — the referral code *is* the referrer's uid, so there's no
  code→user lookup table to keep in sync. The public landing drops a `filmatube.ref` cookie
  (30 days) and points at sign-up.
- **Attribution** is server-side only. On a **new** account's first sign-up the web session route
  (`/api/auth/session`) reads the cookie and writes `referrals/{referredId}`; Android goes through
  `POST /api/referral` with a bearer id-token, since it doesn't use the web session. The cookie is
  cleared either way, so a later sign-in can't re-fire it.
- **`referrals/{}` is keyed by the referred user**, so nobody can be referred twice, and rules
  block *all* client writes — a referral can only come from the trusted server path.
- **Rewards** — `onReferralCreated` grants `referralCount++`, `earlyAccess`, the `recruiter`
  badge, and pushes a notification (inbox + FCM).

### Achievements (200)
`achievements/{uid}/badges/{badgeId}`, function-write / signed-in-read. Nightly **`awardBadges`**
evaluates seven badges and awards + notifies once each:

| Badge | Threshold | Signal |
|---|---|---|
| First Watch | ≥ 1 | completed `watchProgress` count |
| Binge Watcher | ≥ 10 | ” |
| Cinephile | ≥ 25 | ” |
| Critic | ≥ 5 reviews | `stats.reviewsWritten` (kept by `onReviewWritten`) |
| Social Butterfly | ≥ 10 following | `follows` count |
| Premiere Goer | ≥ 1 | `stats.premieresAttended` (kept by `onPremiereAttended`) |
| Recruiter | ≥ 1 referral | `users.referralCount` (also granted immediately) |

Two signals need counters rather than on-the-fly counts: the `items` **collectionGroup is
polluted** (watchProgress/likes/reactions/comments all use `items`), and premiere attendance needs
an `isPremiere` join. Hence the two small triggers.

### Stats + gamification (201–202)
Nightly **`buildStats`** → `stats/{uid}`: `totalWatchMinutes` (counts partial views as
duration × progress, so it's time actually spent), `moviesCompleted`, `topGenres`, plus a day
**streak** and **weekly goal** progress. Written with `merge` so the trigger-maintained counters
survive. Displayed as **stat tiles, not charts** — single headline numbers with no distribution to
plot; genre identity is carried by labels, not colour, so there's no categorical palette to decode.
A shareable **1200×630 PNG** is rendered at `/api/stats-card/{uid}` via `next/og`.

## Design decisions worth keeping
- **Referral code = uid.** No lookup collection, no collision handling, unguessable enough.
- **Server-only referral writes.** The single most important integrity choice: rules deny client
  writes outright, so referral fraud needs a server compromise, not a devtools console.
- **Streak idempotency via `streakLastDay`.** The nightly job records the calendar day it last
  counted; re-running the same day is a no-op. Without this an extra run would silently inflate
  every user's streak.
- **Badges in a subcollection, not a user-doc array.** Day 199 first used an array; Day 200
  migrated to the planned `achievements/{uid}/badges/{badgeId}` so each badge carries `unlockedAt`.
- **Nightly batch over per-write triggers** for badges/stats: recomputing on every watch
  checkpoint (the player writes every ~10s) would be orders of magnitude more invocations.

## Deployed (all `europe-west4`, co-located with the eur3 DB)
`onReferralCreated` · `awardBadges` · `onReviewWritten` · `onPremiereAttended` · `buildStats`
(+ `buildRecommendations` from Week 28). Verified present via `firebase functions:list`.

## Manual checklist — NOT yet executed
Everything below is **built and deployed but not exercised against live data**. This is the
honest state of Week 29:
- [ ] Open `/invite/<your-uid>` in a fresh browser, register a throwaway account, confirm
      `referrals/<new-uid>` appears and the referrer gets the Recruiter notification + badge.
- [ ] Android: open a `filmatube://invite/<uid>` link, register, confirm `/api/referral` fires
      (needs `WEB_API_BASE_URL` reachable from the device — it's `10.0.2.2:3000` in debug).
- [ ] Wait for (or manually trigger) `awardBadges`; the seeded demo accounts have completed watch
      progress and should earn **First Watch** / **Cinephile**.
- [ ] Wait for (or trigger) `buildStats`; confirm hours/movies/top-genres populate and the tiles
      stop reading 0.
- [ ] Load `/api/stats-card/<uid>` and check the PNG renders (first exercise of `next/og` here).
- [ ] Write 5 reviews on one account → `stats.reviewsWritten` climbs → Critic on next sweep.

## Known gaps / follow-ups
- **Badges lag up to 24h.** Only Recruiter is immediate; the rest wait for the nightly sweep.
  Making them instant means calling `evaluateBadges` from per-signal triggers.
- **`earlyAccess` is granted but not consumed.** The flag is set and shown as an unlocked perk,
  but the theater doesn't yet let those users into premieres early.
- **Streaks track *activity*, not *watching*.** They derive from `users.lastActiveAt`, so opening
  the app counts. Watch-only streaks would need a per-day watch log (the player's 10s checkpoints
  overwrite `updatedAt`, so history isn't recoverable after the fact).
- **`weeklyGoal` is fixed at 3** — the schema supports a per-user value; no UI sets it yet.
- **Stats card is public** by uid. It exposes only aggregates that already appear on the public
  profile, but it is not access-gated.

## Week 29 review (Days 197–203)
The growth loop is complete end to end in code: invite → attributed sign-up → reward → badge →
notification, with stats and light gamification layered on top and a shareable card to close the
loop. Integrity was the recurring theme — referrals are unforgeable by construction, badges are
function-awarded, and the streak is idempotent per day.

The honest caveat, unchanged from Week 28: **none of it has run against real users yet.** Both
platforms build green (web `tsc` + `next lint` + 17 Vitest tests; Android `:app:assembleDebug`)
and all functions are deployed, but the first real signal will come from the checklist above.
