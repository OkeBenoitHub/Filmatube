# Player Parity QA — Android ↔ Web (Day 53)

Verifies playback + **watch-progress** behave identically on Android and Web and stay in
sync, because both clients read/write the **same Firestore documents** and stream from the
**same token-protected R2 objects**.

## Shared by design
- **Same stream source:** both fetch `GET /api/stream/{id}` (Android with a Firebase bearer,
  Web with the session cookie) → a 1-hour presigned URL for the private `videos/{videoKey}`
  object. Drafts / coming-soon titles have no playable video.
- **Same progress document:** `watchProgress/{uid}/items/{movieId}` with identical fields —
  `movieId`, `positionMs`, `durationMs`, `progress` (0–1), `completed`, `updatedAt`. Owner-scoped
  by the security rules; written by the Android `WatchProgressRepository` and the Web `WatchPlayer`.
- **Same checkpoint cadence:** every ~10s while playing, plus on pause, on end, and on
  leaving the screen (Android `ON_STOP`; Web tab-hide / `pagehide`).
- **Same mark-watched rule:** `completed = progress ≥ 0.90` on both platforms.
- **Same resume rule:** on re-open, if a saved position exists and the title isn't completed,
  seek there and show a **"Resume from mm:ss / Start over"** prompt (auto-hides after ~6s).
  A completed title starts from 0.
- **Same Continue Watching:** both home screens list in-progress items (completed filtered out),
  newest-first, and tapping resumes at the saved position.

## Cross-platform sync check
- [ ] **Android → Web:** watch ~2 min of a movie on Android, pause. Open the web home →
      the movie shows in **Continue Watching** with a matching progress bar; open it →
      the resume prompt offers the same position.
- [ ] **Web → Android:** scrub to ~50% on web, leave the page. Reopen on Android →
      Home "Continue Watching" shows it; Play resumes near 50%.
- [ ] **Mark watched (90%):** finish (or scrub past 90%) on either platform → the title
      **disappears from Continue Watching on both**, and reopening starts from 0.
- [ ] **Start over:** tap "Start over" in the resume prompt → playback jumps to 0 and the next
      checkpoint overwrites the saved position on both platforms.

## Expected differences (not bugs)
- **Controls:** Android uses a Compose overlay + touch gestures (brightness/volume/seek) + lock;
  Web uses a mouse/keyboard control bar (space, ←/→, ↑/↓, f, m). Both are custom (no native chrome).
- **Fullscreen:** Android immersive system-bar hiding; Web uses the Fullscreen API.
- **PiP:** Android system PiP (auto-enter on leave); Web PiP + mini-player arrive Day 54.
- **Auth to /api/stream:** Android bearer token vs Web session cookie (same `getRequestUser`).

## Status
Both builds green (Android `assembleDebug`; Web `next build`). Runtime checklist is for manual
verification once a movie with an uploaded video + a signed-in account on both platforms is available.
