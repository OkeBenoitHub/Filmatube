# Cross-Device Playback QA + Phase 4 Review (Day 56)

Closes **Phase 4 — Playback Core (Days 43–56)**. Complements `PLAYER_PARITY_QA.md` (Day 53,
feature-by-feature parity) with **cross-device** scenarios: the same account playing on Android
and Web against one Firebase project + one R2 bucket.

## What shipped in Phase 4
**Android player (Week 7, Days 43–49)**
- Media3/ExoPlayer screen; token-protected R2 stream via `/api/stream/{id}`; `KEEP_SCREEN_ON`.
- Custom overlay (`useController=false`): transport, scrub bar, immersive fullscreen.
- Gestures: seek / brightness / volume / double-tap; lock overlay.
- PiP (auto-enter on leave), orientation handling, resize FIT/ZOOM/FILL.
- Watch progress → Firestore (10s + pause/end), resume; Continue Watching row + resume prompt.
- Buffering/error UX + retry; play/pause/complete analytics.

**Web player (Week 8, Days 50–56)**
- HTML5 `<video>` at `/watch/[id]`; token-protected R2 source.
- Custom controls + keyboard shortcuts (space, ←/→, ↑/↓, f, m).
- Watch-progress read/write to the **same** `watchProgress/{uid}/items` docs → Continue Watching.
- Resume prompt + 90% mark-watched parity.
- Mini-player (persists across navigation) + web PiP.
- Open Graph share cards + "Open in App" deep links (`filmatube://`).

## Cross-device checklist (same account)
- [ ] **Resume across devices:** watch ~3 min on Android, pause. On Web home → Continue Watching
      shows it at the right progress; open → resumes at ~3 min. Scrub to ~10 min on Web, leave.
      On Android Home → Continue Watching updated; Play resumes near ~10 min.
- [ ] **Mark-watched removes everywhere:** finish (or scrub past 90%) on one device → the title
      leaves Continue Watching on **both**; reopening starts from 0.
- [ ] **Token URL expiry:** a stream URL is ~1 h; starting playback always requests a fresh URL,
      so a link opened later still plays (no stale 403). Sharing the page URL never shares the video.
- [ ] **Mini-player (web):** start a movie, navigate to /home or /browse → mini-player keeps
      playing; Expand returns to `/watch/[id]`; Close stops it. PiP pops out and survives navigation.
- [ ] **PiP:** Android auto-enters PiP on Home press while playing; Web PiP button pops out the video.
- [ ] **Share + deep link:** Share a movie → link preview shows the backdrop/title (OG card).
      "Open in app" on Android opens the app; on desktop it falls back gracefully.
- [ ] **Auth:** both platforms authenticate to `/api/stream` (Android bearer, Web cookie); signed-out
      access to `/watch/[id]` redirects to login; drafts / coming-soon return not-found.

## Expected differences (not bugs)
- Controls & fullscreen differ by platform (touch/gesture + immersive vs mouse/keyboard + Fullscreen API).
- Android has a native system mini-window (PiP); Web adds an in-page mini-player **and** PiP.
- Deep-link routing into the exact Android screen is best-effort (nested nav); the app always launches.

## Status
Both builds green as of Week 8 close — Android `assembleDebug`, Web `next build` (27 routes).
Phase 4 (Playback Core) complete. Runtime checklist is for manual verification once a movie with an
uploaded video + a signed-in account on both platforms is available.
