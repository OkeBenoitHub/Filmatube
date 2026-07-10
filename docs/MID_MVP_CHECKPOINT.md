# Mid-MVP Checkpoint — Day 84

End of **Phase 6**. This is the halfway-plus review of the v1.0 MVP (Days 1–126): what's shipped,
the state of the backend, and what remains. Both apps build green and are pushed to
`github.com/OkeBenoitHub/Filmatube`.

## Phases complete (Days 1–84)

| Phase | Days | Delivered |
|---|---|---|
| 1 — Foundations | 1–14 | Android (Compose, forced-dark green, Hilt, nav, Firebase, design system, i18n, CI); Web (Next 15, Tailwind tokens, Firebase client+admin, R2 client, presign/stream APIs, schema + rules + indexes, landing, CI) |
| 2 — Auth & Profiles | 15–28 | Email/Google auth both platforms; user docs; taste onboarding; profiles + multi-profiles; avatar→R2; admin gate + shell |
| 3 — Catalog & Admin CMS | 29–42 | Android catalog (home/browse/detail/cast/search); Web Admin CMS (movies list/form, TMDB auto-fill, video+artwork+subtitle upload, publish flow); Web user catalog |
| 4 — Playback Core | 43–56 | Android + Web players; token-protected R2 streaming; custom controls; watch-progress sync; mini-player; PiP; share/deep-links |
| 5 — Playback Advanced | 57–70 | Subtitles + styling; audio-track + speed; sleep timer; Up Next; skip-intro; audio focus/network resilience; admin media-track editors; analytics |
| 6 — Downloads, Offline & Library | 71–84 | Android encrypted offline downloads (queue, Wi-Fi-only, auto-delete, license window, offline playback + subtitles); Watch Later + My Library; Web Collections (create/edit/reorder/share/clone) + My Stuff hub + PWA save-for-later |

## What a user can do today
- **Sign in** (Android + Web, same account), pick taste, manage profiles.
- **Browse & search** a shared movie catalog; view rich detail pages.
- **Admins** create/manage movies end-to-end in the web CMS (metadata, artwork, video, subtitle/audio
  tracks, intro markers).
- **Watch** on either platform with a full custom player (subtitles+styling, audio, speed, sleep
  timer, Up Next, PiP, mini-player), with **watch-progress synced** across devices.
- **Download** movies on Android for **encrypted offline** playback (with offline subtitles),
  manage them (pause/resume/cancel, storage, expiry, auto-delete-watched).
- **Save for later** and build **Collections** (web), shareable and cloneable; **My Stuff** hub.

## Backend / infra state
- Firebase **filmatubelive**: Auth, Firestore (rules + indexes deployed), Analytics, App Check.
- Cloudflare **R2**: 4 buckets (videos private; images/avatars/subtitles public), CORS applied;
  presigned upload + expiring playback URLs; orphan-cleanup script.
- CI: web + Android GitHub Actions.

## Remaining for v1.0 (Days 85–126)
- **Phase 7 — Social Graph & Feed** (85–98): follow/followers, taste-match %, recommend to friends,
  activity feed.
- **Phase 8 — Reviews, Comments, Notifications & Requests** (99–112): ratings/reviews, comments,
  push notifications, content requests.
- **Phase 9 — Polish & Launch Prep** (113–126): admin analytics/moderation, performance, security
  hardening, store listings, launch.

## Open user TODOs (non-blocking)
- Add `GOOGLE_SERVICES_JSON` CI secret (if not done) so Android CI is green.
- Rotate the Firebase service-account key (an old one was shared during setup).
- Apply R2 **lifecycle** rules (CORS already done); optionally an Admin R2 token for
  `apply-r2-config.mjs` / bucket config.
- Android runtime **POST_NOTIFICATIONS** permission request (API 33+) for download notifications.

## Status
Phases 1–6 complete. Android `assembleDebug` and Web `next build` (31 routes) both green.
Halfway checkpoint passed — on to the social layer (Phase 7).
