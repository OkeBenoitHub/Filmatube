# Beta Plan & Web Launch Checklist (Days 123 & 126)

> Play Store is out of scope. Distribution is **web-first**, with the Android app **sideloaded**
> (signed APK) to a small tester group.

## Day 123 — Closed beta (re-scoped)
**Channels**
- **Web preview:** deploy the app to a preview URL (Firebase App Hosting — see below) and share with
  testers. `robots.ts` keeps authed routes out of search.
- **Android:** build a **signed** release APK (`keystore.properties` + `./gradlew assembleRelease`)
  and share the `.apk` directly with testers (no Play Console).

**Monitoring**
- **Crashlytics** (Android) + **Firebase Performance** are wired; watch the console dashboards for
  crash-free rate and slow traces (`stream_url_resolve`, app-start).
- **Web:** Firebase Analytics events (`video_play/pause/complete`, `video_feature`, `social_follow`)
  in the console; server errors via the hosting logs.

**Feedback intake**
- Lightweight to start: the footer **Contact** (`hello@filmatube.app`) and **Report a problem**
  (`support@filmatube.app`) mailto links, plus content requests (`/requests`) already surface user
  reports to the admin queue. A structured in-app feedback form is a fast-follow.

## Day 126 — Web launch (prep + checklist)
### Deploy (Firebase App Hosting)
`WebApp/Filmatube/apphosting.yaml` is committed (runConfig + env→secrets mapping). Steps:
1. `firebase apphosting:backends:create` — connect the GitHub repo, root `WebApp/Filmatube`.
2. Set secrets: `firebase apphosting:secrets:set NEXT_PUBLIC_FIREBASE_API_KEY` … (all vars in the
   yaml; server secrets `FIREBASE_PRIVATE_KEY`, `R2_*`, `TMDB_API_KEY` are RUNTIME-only).
3. Add the custom domain (`filmatube.app`) and set `NEXT_PUBLIC_SITE_URL` accordingly.
4. Push to the connected branch → App Hosting builds `next build` and serves SSR.
5. Point the FCM web push + auth **authorized domains** at the production domain.

### Production rules hardening (pre-launch — from SECURITY_AUDIT)
- [ ] **Split user-doc PII:** move `email` (and any private fields) out of `users/{uid}` into
      `users/{uid}/private/*` so public-profile reads don't expose emails. Touches both clients.
- [ ] **Ban enforcement:** add a `banned` custom claim (admin action) + check it in rules, instead of
      the non-enforced `isBanned` field.
- [ ] Consider a **CSP** (`Content-Security-Policy-Report-Only` → enforce) and **web App Check**
      (reCAPTCHA Enterprise).
- [ ] Enable **App Check enforcement** (Firestore/Auth) once Android Play Integrity + web tokens look
      healthy.

### Launch gate
- [ ] Signed Android APK smoke-tested (R8 release paths: auth, playback, downloads, uploads).
- [ ] Web preview smoke-tested (auth, catalog, player, social, admin).
- [ ] Lighthouse ≥ 90 on landing + /home (see `WEB_PERF_AUDIT.md`).
- [ ] Cloud Functions deployed (`aggregateRatings`, `processScheduledBroadcasts`) — done.
- [ ] Firestore rules + indexes deployed — done.
- [ ] Backups/retention: confirm Firestore PITR / export schedule for production.

### Post-launch
- Monitor Crashlytics crash-free %, Performance traces, hosting error rate, and Firestore/R2 spend
  (see `WEB_TESTS_RULES_COST.md`) for the first 72h; hotfix via the same CI → deploy path.

## Status
Beta channels + monitoring defined (Day 123 ready). Hosting config committed and launch checklist +
production-hardening items enumerated (Day 126 prep). **Actual deploy, tester recruitment, and the
PII/ban rule changes are the remaining owner actions.**
