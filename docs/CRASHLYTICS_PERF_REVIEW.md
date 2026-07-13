# Crashlytics & Performance Review (Day 115)

## Crashlytics
- Already enabled; collection **off in debug**, on in release (`setCrashlyticsCollectionEnabled(!DEBUG)`).
- Added `data/analytics/CrashReporter` — a thin injectable wrapper (`recordNonFatal`, `log`,
  `setKey`) so errors that `runCatching` would otherwise swallow become visible **non-fatals** with
  breadcrumbs.
- Wired into the **player load** failure path (`PlayerViewModel`), the most user-visible error:
  a failed stream-URL resolve / prepare now records a non-fatal tagged with `movieId` instead of
  silently showing the retry view. (More catch sites — downloads, uploads — can adopt it as issues
  surface in the dashboard.)

## Performance Monitoring
- Added the **Firebase Performance** SDK + Gradle plugin (`com.google.firebase.firebase-perf`).
  Out of the box this collects app-start time, foreground/background sessions, screen rendering
  (slow/frozen frames) and all OkHttp/HttpURLConnection network requests.
- Added a custom trace **`stream_url_resolve`** around the presign + network call in
  `PlaybackRepository`, with an `http_code` metric — this is the latency that gates "time to first
  frame", so it's the highest-value custom trace to watch.

## Top issues found & fixed (Phase 9 hardening pass)
1. **Release build was broken** (latent) — `DebugAppCheckProviderFactory` (`debugImplementation`)
   referenced unconditionally, so `assembleRelease` never compiled. Fixed via variant source sets
   (Day 114). This also keeps the debug App Check provider out of release APKs.
2. **APK bloat** — R8 + resource shrinking now enabled: ~28 MB → ~6 MB (Day 114).
3. **Unbounded realtime reads** — reviews/comments observers now capped (Day 113).
4. **Silent playback failures** — now reported as Crashlytics non-fatals (this day).

## What to watch after release
- Crashlytics **crash-free users** rate; triage any `player load failed` non-fatals by `movieId`.
- Performance dashboard: `stream_url_resolve` P90, app-start (cold) duration, slow/frozen frames on
  Home/Browse (heavy image lists — now Coil-cached).
- If cold-start needs more: add a **Baseline Profile** (macrobenchmark) as noted in
  `PERF_APP_AUDIT.md`.

## Status
`assembleDebug` and `assembleRelease` both green with Performance Monitoring enabled. Closes the
Day 113–115 Android hardening pass.
