# Filmatube — Release Roadmap (Versioned · Full Daily Plan)

> Fully detailed, day-by-day build plan organized into **shippable release versions**. Companion to `SCHEDULE.md` (the linear 182-day plan); this is the canonical versioned schedule. Cadence: **alternating — odd weeks = Android, even weeks = Web**. Movies-first; **TV shows & animes are a dedicated v2.0**.

**Vision:** *"Netflix + social."* Watch & download movies offline, **follow people who share your taste**, **discuss in real time**, **recommend** titles, like, save for later, and watch together in an **Online Movie Theater**.

**Stack (summary):**
- **Android:** Kotlin + Jetpack Compose, Material 3 **forced-dark green**, Hilt, Coroutines/Flow, Navigation-Compose, Media3/ExoPlayer (custom overlay `useController=false`), Coil, Room, DataStore, WorkManager; Cast SDK + Android TV later. Firebase: Auth/Firestore/FCM/Crashlytics/Performance/App Check.
- **Web:** Next.js 15 + React 19 + TS, Tailwind 3 (green `brand`, `darkMode:'class'`), `clsx`+`tailwind-merge`, `lucide-react`, `firebase`+`firebase-admin`, HTML5/Plyr-style video → `hls.js` later, PWA, Firebase Hosting.
- **Backend:** Firestore (metadata/social — URLs only). **Cloudflare R2** for all binaries (video, images, avatars, subtitles) via **presigned uploads** + **expiring token-protected playback URLs**; presign/admin-gating in Next.js API routes. TMDB metadata. FCM push. Cloud Functions (rating aggregation, theater sync clock, feed cleanup, broadcast fan-out). **Payments: Google Play Billing (Android) + FastSpring (web, merchant-of-record).**

---

## Release Overview

| Version | Days | Weeks | Headline | Ships when users get… |
|---|---|---|---|---|
| **v1.0** | 1–126 | 1–18 | **Core (MVP)** | Watch + download offline, follow/taste-match, recommend, like, watch-later, reviews, requests |
| **v1.1** | 127–154 | 19–22 | **Community** | Discussion boards, real-time chat, private watch parties |
| **v1.2** | 155–182 | 23–26 | **Online Movie Theater** | Public scheduled showtimes & premieres, synced playback, live chat |
| **v1.3** | 183–210 | 27–30 | **Discovery & Growth** | "Because you watched…" rows, referral/invite, gamification |
| **v1.4** | 211–238 | 31–34 | **Living Room & Polish** | Chromecast, Android TV, PWA, HLS adaptive streaming |
| **v1.5** | 239–252 | 35–36 | **Family** | Kids profiles + parental controls |
| **v2.0** | 253–308 | 37–44 | **Series & Anime** | TV shows + animes: browse/watch/download/boards/theater |
| **v2.1** | 309–336 | 45–48 | **Monetization** | Subscriptions: Play Billing + FastSpring |
| **v2.2+** | backlog | — | **Future** | ML recs, public theaters at scale, iOS, ads tier |

## Where each "extra idea" lives
| Idea | Scheduled in |
|---|---|
| Taste Match % | v1.0 (Days 86, 96) |
| Expiring token-protected R2 URLs | v1.0 (Day 43); hardened v1.0 (Day 121), v1.4 |
| Online Movie Theater premieres | v1.2 (Days 169–171) |
| Kids profiles + parental controls | **v1.5 (dedicated)** |
| "Because you watched…" rows | **v1.3 (dedicated recsys)** |
| Referral / invite-a-friend | **v1.3 (dedicated)** |
| Download encryption | v1.0 (Day 76) |
| Chromecast + Android TV | **v1.4 (dedicated)** |
| Monetization (Play Billing + FastSpring) | **v2.1 (dedicated)** |
| TV shows & animes (full) | **v2.0 (dedicated)** |

## Legend
`[ ]` not started · `[~]` in progress · `[x]` done · **(A)** Android · **(W)** Web · **(B)** Backend/shared

---

# ███ RELEASE v1.0 — CORE (MVP) ███  (Days 1–126)
*A complete, launchable movie-streaming + social app: watch, download offline, follow people who share your taste, recommend, like, save, review, request.*

## PHASE 1 — Foundations (Days 1–14)

### Week 1 — Android Foundation (Days 1–7) (A)
- **Day 1** ✅ — Compose project; `ui/domain/data/di` packages; Gradle version catalog; AGP/Kotlin/Compose BOM; git init + `.gitignore`.
- **Day 2** ✅ — Material 3 **forced-dark green** theme (tokens from logo), typography, shapes; adaptive app icon + splash from logo.
- **Day 3** ✅ — Hilt; `NetworkModule`/`FirebaseModule`/`RepositoryModule`; `DataState<T>`; `BaseViewModel`; Result/error handling.
- **Day 4** ✅ — Navigation-Compose graph; bottom nav (Home, Search, **Theater**, Community, Profile); placeholder screens.
- **Day 5** ✅ — Firebase wiring (`google-services.json`; Auth/Firestore/FCM/Crashlytics/App Check); Retrofit/OkHttp; Coil; DataStore.
- **Day 6** ✅ — Compose design system: buttons, cards, chips, poster tile, content rows, section header, shimmer, empty/error/loading, snackbars.
- **Day 7** ✅ — Localization scaffold (EN + FR); on-device theme QA; smoke build; **CI** (GitHub Actions `assembleDebug`).

### Week 2 — Web Foundation + Shared Backend (Days 8–14) (W/B)
- **Day 8** ✅ — Next.js 15 project (mirror old `package.json`); TS + ESLint; layout `/app`, `/app/admin`, `/app/watch`, `/lib`, `/components`.
- **Day 9** ✅ — Tailwind **green `brand` + dark `surface`** tokens; base components (Button/Card/Input/Modal/Toast/Skeleton).
- **Day 10** ✅ — Firebase client (`lib/firebase.ts`) + admin SDK (`lib/firebase-admin.ts`); `.env.local`; session-cookie helper.
- **Day 11 (B)** ✅ — R2 buckets (`videos`,`images`,`avatars`,`subtitles`); `lib/r2.ts` S3 client; CORS; lifecycle rules.
- **Day 12 (B)** ✅ — Presign API: `POST /api/uploads/presign` (admin-gated), `GET /api/stream/[id]` (expiring token URL), image/avatar presign.
- **Day 13 (B)** ✅ — **Finalize movies-only Firestore schema** + **security rules v1** + composite indexes file.
- **Day 14** ✅ — Landing skeleton (hero/features/store buttons); web **CI** (lint/build); week review.

## PHASE 2 — Auth & Profiles (Days 15–28)

### Week 3 — Android Auth & Profiles (Days 15–21) (A)
- **Day 15** ✅ — Splash + animated logo; 3-slide onboarding.
- **Day 16** ✅ — Register/Login (Email/Password + Google); validation + error states.
- **Day 17** ✅ — Forgot-password flow; create `users/{uid}` on first login; App Check tokens.
- **Day 18** ✅ — Taste onboarding: genre picker, language (EN/FR), content prefs.
- **Day 19** ✅ — Profile screen: avatar, bio, stats, followers/following, badges placeholder.
- **Day 20** ✅ — Edit profile; **avatar upload → R2** (presign → PUT → store URL).
- **Day 21** ✅ — Netflix-style multi-profiles per account; language toggle; week review.

### Week 4 — Web Auth, Profiles & Admin Shell (Days 22–28) (W)
- **Day 22** ✅ — Web Auth (Email/Password + Google); session cookies via admin SDK; route-protection middleware.
- **Day 23** ✅ — Register/login/forgot pages; create user doc.
- **Day 24** ✅ — Web profile + edit; avatar → R2.
- **Day 25** ✅ — Taste onboarding (web); profile switcher.
- **Day 26** ✅ — **Admin auth** (`isAdmin` gate); admin layout + sidebar (Dashboard, Movies, Users, Requests, Theater, Notifications, Analytics).
- **Day 27** ✅ — Admin dashboard skeleton (stat cards, recent-activity placeholders).
- **Day 28** ✅ — Auth parity QA (Android ↔ web, same account); week review.

## PHASE 3 — Movie Catalog & Admin CMS (Days 29–42)

### Week 5 — Android Movie Catalog (Days 29–35) (A)
- **Day 29** ✅ — Domain models (`Movie`,`Genre`,`CastMember`); `MovieRepository` queries (featured, trending, new, by-genre, detail, search).
- **Day 30** ✅ — Home: hero banner (auto-rotate), Trending, New Releases, Continue Watching (stub), By-genre rows.
- **Day 31** ✅ — Browse grid + genre chips; sort (newest/rating/A–Z); year filter.
- **Day 32** ✅ — Movie detail: backdrop, poster, meta (year/runtime/age rating), genres, overview, average rating.
- **Day 33** ✅ — Cast row + tap → actor filmography (Firestore search); "More like this" related row.
- **Day 34** ✅ — Search: debounced; filters (genre/year/rating); recent + trending searches.
- **Day 35** ✅ — Trailer (YouTube intent); **Coming Soon** row + reminder bell; paging/caching; week review.

### Week 6 — Web Admin CMS: Movies + R2 Upload (Days 36–42) (W)
- **Day 36** ✅ — Movies list: paginated/searchable/sortable; featured/pinned/coming-soon badges; status (draft/published).
- **Day 37** ✅ — Add/Edit movie form; **TMDB auto-fill** (IMDB/TMDB ID → title/overview/cast/runtime/genres/artwork/age rating).
- **Day 38** ✅ — **Video upload to R2** (multipart presigned) with progress; store `videoKey`; generate token playback URL; replace-video flow.
- **Day 39** ✅ — Poster/backdrop/thumbnail upload to R2; **subtitle (.vtt) upload per language**; trailer URL field.
- **Day 40** ✅ — Featured/pinned/coming-soon toggles; bilingual (en/fr) title+description; delete-with-confirm; publish/unpublish.
- **Day 41** ✅ — Web user catalog (home rows, browse, detail, search) — mirrors Android.
- **Day 42** — Catalog parity QA; week review.

## PHASE 4 — Playback Core (Days 43–56)

### Week 7 — Android Player Core (Days 43–49) (A)
- **Day 43** — Media3/ExoPlayer screen; fetch **expiring token R2 URL**; progressive MP4 playback; `KEEP_SCREEN_ON`.
- **Day 44** — Custom overlay (`useController=false`): play/pause, rewind/FF (10s), seek bar with thumbnails, fullscreen (immersive sticky).
- **Day 45** — Swipe gestures: horizontal = seek, vertical-left = brightness, vertical-right = volume, double-tap = ±10s; lock-screen overlay.
- **Day 46** — PiP (`onUserLeaveHint`/`onPipModeChanged` hide controls); orientation handling; resize modes (fit/fill/zoom).
- **Day 47** — Save watch position to Firestore every ~10s; mark watched at 90%; `watchProgress` update.
- **Day 48** — Continue Watching row wired; resume prompt on re-open.
- **Day 49** — Buffering/error UX; retry; analytics (play/pause/complete); week review.

### Week 8 — Web Player Core + Cross-Device (Days 50–56) (W)
- **Day 50** — HTML5 `<video>` page; expiring token R2 source.
- **Day 51** — Custom controls: play/pause/seek/volume/fullscreen; keyboard shortcuts.
- **Day 52** — Watch-progress write/read; **Continue Watching synced with Android** via Firestore.
- **Day 53** — Resume prompt; mark-watched parity (90%).
- **Day 54** — Mini-player (keep browsing while watching); PiP (web).
- **Day 55** — "Continue on Web / Open in App" deep links; share movie links (Open Graph cards).
- **Day 56** — Cross-device playback QA; week review.

## PHASE 5 — Playback Advanced (Days 57–70)

### Week 9 — Android Player Advanced (Days 57–63) (A)
- **Day 57** — Subtitles: fetch `.vtt` from R2; ExoPlayer text-track injection; **subtitle language selector**.
- **Day 58** — **Subtitle style settings** (size/color/background/edge/position); persist via DataStore.
- **Day 59** — **Audio-track selector** (EN/FR multi-audio); playback-speed control.
- **Day 60** — **Sleep timer** dialog (15/30/45/60 + end-of-movie); badge countdown; auto-pause.
- **Day 61** — **"Up Next" recommended-movie** autoplay (last 30s → 10s countdown, cancelable); optional **skip-intro** (admin markers).
- **Day 62** — Background-audio handling; network-loss resume; gesture refinement.
- **Day 63** — Player polish + analytics; week review.

### Week 10 — Web Player Advanced + Admin Media Tracks (Days 64–70) (W)
- **Day 64** — Plyr-style web controls (settings gear, quality menu placeholder, speed, captions toggle).
- **Day 65** — Web subtitles (.vtt) + style options; audio-track switching.
- **Day 66** — Web sleep timer / autoplay "Up Next".
- **Day 67** — Admin: manage **subtitle tracks** per movie (add/remove languages) + **audio tracks**; **intro-markers editor**.
- **Day 68** — Admin: replace/re-upload video; R2 object management + orphan cleanup tool.
- **Day 69** — Web player resume/network resilience; analytics events.
- **Day 70** — Player parity QA (subtitles/audio/progress Android↔web); phase review.

## PHASE 6 — Downloads, Offline & Library (Days 71–84)

### Week 11 — Android Downloads & Offline (Days 71–77) (A)
- **Day 71** — Media3 `DownloadManager` → R2 MP4 to local; download-quality preference.
- **Day 72** — Download queue UI: progress, pause/resume/cancel; Room persistence; WorkManager (Wi-Fi-only constraint).
- **Day 73** — My Downloads screen; downloaded-movie tracker; storage-usage indicator.
- **Day 74** — Offline playback from local file + offline subtitles; offline watch-progress (sync on reconnect).
- **Day 75** — Delete individual/all; auto-delete-watched toggle; download expiry/license window.
- **Day 76** — **Download encryption** (encrypt offline files; key in Android Keystore).
- **Day 77** — Watch Later + My Library (Android) wiring; week review.

### Week 12 — Web Library & Collections (Days 78–84) (W)
- **Day 78** — Watch Later (web); My Library page.
- **Day 79** — Collections: create/edit; cover → R2; add movies.
- **Day 80** — Share collections (public link + OG cards); save others' public collections.
- **Day 81** — Collection detail + reorder; "My Stuff" hub (watchlist + collections + continue watching).
- **Day 82** — PWA "save for later" (service-worker cache, best-effort); downloads info page.
- **Day 83** — Library parity QA; bidirectional sync (watch later/collections Android↔web).
- **Day 84** — Phase review; mid-MVP checkpoint.

## PHASE 7 — Social Graph & Feed (Days 85–98)

### Week 13 — Android Social (Days 85–91) (A)
- **Day 85** — Follow/unfollow; followers/following screens.
- **Day 86** — Follow suggestions (taste overlap) + **Taste Match %** calculation/caching.
- **Day 87** — Activity Feed (real-time listener): events (watching/watched/liked/reacted/added-watchlist/added-collection).
- **Day 88** — Feed time filter; pagination; mute/hide.
- **Day 89** — Likes + reactions (love/fire/mind-blown/boring); reaction counts on detail; FCM notify followers.
- **Day 90** — Recommend movie to a follower + message; recommendations inbox; FCM.
- **Day 91** — Public profile (their lists/activity/taste match); analytics; week review.

### Week 14 — Web Social (Days 92–98) (W)
- **Day 92** — Follow + followers/following (web).
- **Day 93** — Activity feed real-time (web) + filters.
- **Day 94** — Likes/reactions parity; counts on detail.
- **Day 95** — Recommend + inbox (web).
- **Day 96** — Public profiles + **Taste Match %** badge.
- **Day 97** — Follow suggestions (web); "people with your taste" discovery.
- **Day 98** — Social parity QA; week review.

## PHASE 8 — Reviews, Comments, Notifications & Requests (Days 99–112)

### Week 15 — Android Reviews + Notifications (Days 99–105) (A)
- **Day 99** — Star ratings (1–5); aggregation (Cloud Function → `averageRating`/`ratingsCount`).
- **Day 100** — Written reviews (public on detail, spoiler-tag); review likes.
- **Day 101** — Threaded comments; comment likes; report review/comment; spoiler-free mode.
- **Day 102** — FCM token registration; notification channels (social/content/system).
- **Day 103** — All notification types wired + tested end-to-end.
- **Day 104** — In-app notification center: unread badge, grouping, mark-all-read, deep links.
- **Day 105** — Notification preferences screen; week review.

### Week 16 — Web Reviews + Push + Requests + Broadcast (Days 106–112) (W)
- **Day 106** — Ratings + reviews (web); spoiler handling.
- **Day 107** — Threaded comments (web); report.
- **Day 108** — Web push (FCM) + notification center.
- **Day 109** — Notification preferences (web); report-moderation queue (admin).
- **Day 110** — **Admin broadcast** composer: segment (all/taste/active), schedule send, preview; Cloud Function fan-out.
- **Day 111** — Content Request form (user) + **admin requests queue** (approve/reject + reason → FCM); link request → added movie.
- **Day 112** — Parity QA; week review.

## PHASE 9 — Hardening, Beta & Launch (Days 113–126)

### Week 17 — Android Hardening & Testing (Days 113–119) (A)
- **Day 113** — Firestore query optimization + composite indexes (Android paths); pagination audit.
- **Day 114** — Image/caching audit (Coil); memory/leak audit; APK size; cold-start.
- **Day 115** — Crashlytics + Performance review; fix top issues.
- **Day 116** — Unit tests (ViewModels/repos) + critical UI/E2E flows; CI gating.
- **Day 117** — Security: App Check enforcement; download-encryption verification; rules audit.
- **Day 118** — Accessibility pass (TalkBack, contrast, touch targets); FR localization sweep.
- **Day 119** — Android release-candidate build; week review.

### Week 18 — Web Hardening + Beta + **v1.0 LAUNCH** (Days 120–126) (W/B)
- **Day 120** — Web performance (code-split, image optimization, caching headers, Lighthouse).
- **Day 121** — Security (web): App Check, session hardening, presign rate-limiting, R2 anti-hotlink/expiry audit.
- **Day 122** — Web E2E tests + CI gating; Firestore rules final audit; cost review.
- **Day 123** — Closed beta (Play Internal Testing + web preview); Crashlytics monitoring; feedback intake.
- **Day 124** — Beta fixes; landing page final (hero/features/screenshots/store buttons), SEO/OG, privacy + terms.
- **Day 125** — Play Store listing (icon, screenshots EN/FR, description, content rating, data-safety); Firebase Hosting deploy.
- **Day 126** — **🚀 v1.0 LAUNCH**; production rules hardening; post-launch monitoring + retro.

---

# ███ RELEASE v1.1 — COMMUNITY ███  (Days 127–154)
*Real-time discussion + watch together (private). Delivers the "discuss movies in real time" pillar.*

### Week 19 — Android Boards & Chat (Days 127–133) (A)
- **Day 127** — Boards discovery (tabs + featured); board types (movies/general).
- **Day 128** — Create board (title/desc/cover → R2, public/private); My Boards.
- **Day 129** — Join/leave; invite followers (FCM).
- **Day 130** — Real-time board chat (Firestore listener); send/typing; spoiler tag.
- **Day 131** — Emoji reactions; reply threads; share movie card into board.
- **Day 132** — Member list + roles + moderation (mute/remove/pin); report message.
- **Day 133** — Board notifications + deep links; week review.

### Week 20 — Web Boards & Chat (Days 134–140) (W)
- **Day 134** — Boards discovery + create (web).
- **Day 135** — Real-time chat (web); reactions/threads parity.
- **Day 136** — Member roles + moderation; **admin official boards**.
- **Day 137** — Share movie card; spoiler tags; board search.
- **Day 138** — Board moderation queue (admin); report handling.
- **Day 139** — Boards parity QA (Android↔web).
- **Day 140** — Week review.

### Week 21 — Android Watch Parties (private) (Days 141–147) (A)
- **Day 141** — Watch Party data model + **shared sync engine** (host writes position/state, guests read).
- **Day 142** — Create party (pick movie, set time); invite board members/followers (FCM).
- **Day 143** — Synced playback room; drift correction; guest catch-up.
- **Day 144** — Floating chat + emoji reactions overlay during playback.
- **Day 145** — Host controls (pause/seek/skip); guest follow.
- **Day 146** — Leave/end party; edge cases (host drops → handoff/end).
- **Day 147** — Week review.

### Week 22 — Web Watch Parties + **v1.1 Release** (Days 148–154) (W)
- **Day 148** — Watch Parties (web) — reuse sync engine: create/invite.
- **Day 149** — Synced playback room (web) + floating chat/reactions.
- **Day 150** — Host controls + guest follow (web).
- **Day 151** — Parties parity QA; sync stress test (many guests).
- **Day 152** — Hardening + Crashlytics review.
- **Day 153** — Beta + fixes.
- **Day 154** — **🚀 v1.1 release** (Play + web); monitoring.

---

# ███ RELEASE v1.2 — ONLINE MOVIE THEATER ███  (Days 155–182)
*Public scheduled cinema — showtimes & premieres with synced playback and live chat.*

### Week 23 — Theater (Android) (Days 155–161) (A)
- **Day 155** — Theater tab: upcoming showtimes/premieres (poster, countdown, attendees).
- **Day 156** — Showtime detail + RSVP/remind (FCM).
- **Day 157** — Lobby (countdown, attendee avatars, pre-show chat).
- **Day 158** — Synced playback room (server-clock driven; no scrubbing; catch-up).
- **Day 159** — Live chat (rate-limited, spoiler-tag) + floating emoji reactions overlay.
- **Day 160** — "Now Showing" live indicator; join mid-show; presence/attendee count.
- **Day 161** — Theater notifications ("starting soon", "friend in a theater"); week review.

### Week 24 — Theater (Web) + Admin (Days 162–168) (W)
- **Day 162** — Theater discovery + showtime detail (web); RSVP/remind.
- **Day 163** — Lobby + pre-show chat (web).
- **Day 164** — Synced playback room (web, server clock); live chat + reactions.
- **Day 165** — Now Showing / join mid-show; attendee presence.
- **Day 166** — **Admin Theater CMS**: schedule showtimes (movie/datetime/capacity/public); lineup mgmt.
- **Day 167** — Admin host controls (pause/skip/end); attendance analytics.
- **Day 168** — Week review.

### Week 25 — Premieres + Automation (Days 169–175) (B/A)
- **Day 169** — **Premiere** type: first public screening of a new release; premiere badge/branding.
- **Day 170 (B)** — Theater automation (Cloud Function): auto lobby → live → ended; sync clock; recurring showtimes.
- **Day 171** — Post-show → auto-created discussion board; "rate it now" prompt.
- **Day 172** — Capacity/waitlist handling; private (board-only) theaters.
- **Day 173** — Android theater polish + edge cases.
- **Day 174** — Concurrency/load testing (sync clock under many viewers).
- **Day 175** — Week review.

### Week 26 — Theater QA + **v1.2 Release** (Days 176–182) (W/B)
- **Day 176** — Theater parity QA (Android↔web).
- **Day 177** — Performance + Firestore cost review (chat/presence fan-out).
- **Day 178** — Security: theater rules audit; chat moderation.
- **Day 179** — Beta + fixes.
- **Day 180** — Store/web release prep.
- **Day 181** — **🚀 v1.2 release**; monitoring.
- **Day 182** — Retro + buffer.

---

# ███ RELEASE v1.3 — DISCOVERY & GROWTH ███  (Days 183–210)
*Keep people watching ("Because you watched…"), bring friends in (referral), reward engagement (gamification).*

### Week 27 — Recommendations Engine (Android) (Days 183–189) (A)
- **Day 183** — Per-user rec doc design (`recs/{userId}`) built by scheduled Cloud Function — genre/cast/people overlap, **no ML**.
- **Day 184** — "Because you watched X" rows on Home; "More like this" on detail.
- **Day 185** — "Top picks for you" row (weighted by taste profile + history).
- **Day 186** — "Trending among people you follow" (social recommendation row).
- **Day 187** — "Hidden gems" / "New for you" rows; row ordering personalization.
- **Day 188** — Rec feedback ("not interested", "seen it") → tune future recs.
- **Day 189** — Week review.

### Week 28 — Recommendations (Web) + Admin Curation (Days 190–196) (W)
- **Day 190** — Recs rows on web home + detail parity.
- **Day 191** — "Because you watched / Top picks / From people you follow" (web).
- **Day 192** — Rec feedback controls (web).
- **Day 193** — **Admin curation tools**: manual row builder, pin/boost titles, schedule home-row campaigns.
- **Day 194** — Editorial collections (admin-curated, featured on home).
- **Day 195** — Recs QA + relevance tuning.
- **Day 196** — Week review.

### Week 29 — Referral + Gamification (Android) (Days 197–203) (A)
- **Day 197** — **Referral**: invite link/code + deep link; `referrals/{}` tracking.
- **Day 198** — Invite-a-friend UI (share sheet, contacts); referral status screen.
- **Day 199** — Referral rewards (badge, early premiere access); FCM on successful referral.
- **Day 200** — Achievement engine + badges (First Watch, Binge Watcher, Cinephile, Critic, Social Butterfly, Premiere Goer, **Recruiter**).
- **Day 201** — Stats dashboard: watch hours, movies completed, top genres; badges + stats on profile.
- **Day 202** — Streaks / weekly goals (light gamification); shareable stats card.
- **Day 203** — Week review.

### Week 30 — Referral + Gamification (Web) + **v1.3 Release** (Days 204–210) (W)
- **Day 204** — Referral on web (link/code, landing attribution, reward grant).
- **Day 205** — Gamification (badges/stats) on web profile.
- **Day 206** — Admin: referral analytics + abuse/fraud guard (self-referral, multi-account).
- **Day 207** — QA (recs + referral + gamification parity).
- **Day 208** — Hardening + Crashlytics review.
- **Day 209** — Beta + fixes.
- **Day 210** — **🚀 v1.3 release**; monitoring.

---

# ███ RELEASE v1.4 — LIVING ROOM & POLISH ███  (Days 211–238)
*Big-screen viewing + adaptive streaming + overall polish.*

### Week 31 — Chromecast + Android TV (part 1) (Days 211–217) (A)
- **Day 211** — Chromecast (Cast SDK): cast button in player; cast R2 token stream.
- **Day 212** — Cast session position sync; mini-controller; subtitles/audio on cast.
- **Day 213** — Cast from Continue Watching / detail; notification controls.
- **Day 214** — Android TV: leanback home/browse UI.
- **Day 215** — Android TV: detail + D-pad navigation.
- **Day 216** — Android TV: full playback + subtitles/audio.
- **Day 217** — Week review.

### Week 32 — PWA + Web Push + Marketing (Days 218–224) (W)
- **Day 218** — PWA manifest + service worker; installable; offline shell.
- **Day 219** — Web push (new movies/recs/showtimes); Continue Watching on web home.
- **Day 220** — "Continue on Web" / "Open in App" deep links.
- **Day 221** — Landing/marketing refresh; feature pages; screenshots.
- **Day 222** — SEO/OG/sitemap; analytics dashboard depth (DAU/MAU, top movies, theater attendance).
- **Day 223** — Web responsive/polish; account deletion + data export (compliance).
- **Day 224** — Week review.

### Week 33 — Android TV (part 2) + Polish & A11y (Days 225–231) (A)
- **Day 225** — Android TV: search + recommendations row.
- **Day 226** — Android TV: watch parties/theater (lean-back read-only or join).
- **Day 227** — Shared-element transitions (poster → detail); micro-animations.
- **Day 228** — Shimmer/skeleton pass; Material 3 accenting within green theme.
- **Day 229** — Tablet/responsive + landscape layouts.
- **Day 230** — Accessibility deep pass (TalkBack, focus order, captions, contrast); full FR review.
- **Day 231** — Week review.

### Week 34 — HLS Pipeline + **v1.4 Release** (Days 232–238) (W/B)
- **Day 232 (B)** — **HLS pipeline**: FFmpeg transcode worker on upload → multi-bitrate `.m3u8` + segments to R2 (MP4 fallback).
- **Day 233** — `hls.js` (web) + Media3 HLS (Android) wired to renditions; ABR testing.
- **Day 234** — Backfill transcode for existing catalog; admin re-encode controls.
- **Day 235** — Streaming QA (start time, seeking, quality switching, bandwidth).
- **Day 236** — Hardening + perf.
- **Day 237** — Beta + fixes.
- **Day 238** — **🚀 v1.4 release**; monitoring.

---

# ███ RELEASE v1.5 — FAMILY ███  (Days 239–252)
*Safe, age-appropriate viewing for kids; parental controls.*

### Week 35 — Kids Profiles + Parental Controls (Android) (Days 239–245) (A)
- **Day 239** — Kids-profile type on the existing multi-profile system; kid-safe simplified UI (no social/boards/chat/requests).
- **Day 240** — **Age-rating ceiling** per profile; content filtering by `ageRating` across home/browse/search.
- **Day 241** — **Parental PIN**: lock profile switching; PIN to exit kids mode; reset via email.
- **Day 242** — Watch-time limits / bedtime; screen-time summary for parent.
- **Day 243** — Kid-safe player (no autoplay-to-mature, simplified controls); curated kids rows.
- **Day 244** — Parent dashboard: view kid activity, approve titles, set limits.
- **Day 245** — Week review.

### Week 36 — Kids (Web) + Admin + **v1.5 Release** (Days 246–252) (W)
- **Day 246** — Kids profile + age gate + content filtering (web).
- **Day 247** — Parental PIN + parent dashboard (web).
- **Day 248** — **Admin**: enforce `ageRating` required on movies; kids-eligible flag; curated kids catalog.
- **Day 249** — Compliance review (children's privacy: COPPA/GDPR-K; no data collection in kids mode).
- **Day 250** — QA (filtering correctness — no mature leakage), parity.
- **Day 251** — Beta + fixes.
- **Day 252** — **🚀 v1.5 release**; monitoring.

---

# ███ RELEASE v2.0 — SERIES & ANIME ███  (Days 253–308)
*Add TV shows + animes as first-class content with seasons/episodes — browse, watch, download, discuss, theater. Major release.*

### Week 37 — Generalize Content Model + Shows Schema (Days 253–259) (B/W)
- **Day 253 (B)** — Generalize to `type: movie | tvshow | anime`; add `tvshows/*`, `animes/*` collections (shared shape).
- **Day 254 (B)** — `seasons` + `episodes` subcollections; episode-level `videoKey`, `introStart/End`, `recapEnd`, `subtitleTracks`, `airDate`.
- **Day 255 (B)** — Security rules + indexes for show/season/episode queries.
- **Day 256 (W)** — Admin CMS: TV Show / Anime create + edit (TMDB auto-fill by show ID).
- **Day 257 (W)** — Admin: seasons hierarchy CRUD.
- **Day 258 (W)** — Admin: episode form (number/title/desc/thumb/duration/IMDB/air date) + video upload to R2.
- **Day 259** — Week review.

### Week 38 — Admin Episodes + Web Browse (Days 260–266) (W)
- **Day 260** — Admin: **bulk episode import** per season; new-episode notification trigger.
- **Day 261** — Admin: per-episode subtitle/audio uploads; intro/recap markers editor.
- **Day 262** — Web browse: Shows + Animes tabs/grids; genre filters.
- **Day 263** — Web show detail: seasons selector, episode list, meta.
- **Day 264** — Web episode playback (reuse player) + next-episode autoplay.
- **Day 265** — Web downloads for episodes; per-episode watch progress.
- **Day 266** — Week review.

### Week 39 — Android Browse Shows/Animes (Days 267–273) (A)
- **Day 267** — Add Shows + Animes to Home rows + bottom-nav browse tabs.
- **Day 268** — Show/Anime detail: backdrop, meta, genres, cast.
- **Day 269** — **Season picker** + episode list (bottom sheet).
- **Day 270** — Continue-watching per show (next-up episode); resume to correct episode.
- **Day 271** — Search across all types (movies/shows/animes) with type filter.
- **Day 272** — "More like this" across types.
- **Day 273** — Week review.

### Week 40 — Android Episode Playback (Days 274–280) (A)
- **Day 274** — Episode playback from R2; episode subtitles/audio.
- **Day 275** — **Next-episode autoplay** (last 30s → countdown, cancelable).
- **Day 276** — **Skip intro / skip recap** (episode markers).
- **Day 277** — Binge tracking; mark season/episode watched; show progress %.
- **Day 278** — New-episode FCM → deep link to episode.
- **Day 279** — Per-episode downloads (queue/offline/encryption reuse).
- **Day 280** — Week review.

### Week 41 — Shows in Social/Boards (Android) (Days 281–287) (A)
- **Day 281** — Reviews/ratings/comments for shows + per-season/episode where relevant.
- **Day 282** — Likes/reactions + recommend (shows/animes) in feed.
- **Day 283** — **Per-show discussion boards** (auto official board per popular show); episode-thread spoiler tags.
- **Day 284** — Watch-later/collections support shows + animes.
- **Day 285** — Taste-match + recs include shows/animes.
- **Day 286** — Theater supports episode screenings (premiere new episodes).
- **Day 287** — Week review.

### Week 42 — Shows in Social/Boards (Web) (Days 288–294) (W)
- **Day 288** — Web reviews/comments for shows/episodes.
- **Day 289** — Web likes/reactions/recommend (shows/animes).
- **Day 290** — Web per-show discussion boards + episode threads.
- **Day 291** — Web watch-later/collections for shows/animes.
- **Day 292** — Web recs include shows/animes; episode "Up Next".
- **Day 293** — Web theater episode screenings.
- **Day 294** — Week review.

### Week 43 — Android TV / Cast for Shows + Polish (Days 295–301) (A)
- **Day 295** — Android TV: shows/animes browse + season/episode nav.
- **Day 296** — Cast episodes (Chromecast); next-episode on cast.
- **Day 297** — Kids mode: kid-safe shows/animes filtering by age rating.
- **Day 298** — Performance (large episode lists, paging).
- **Day 299** — Migration check (existing movie data untouched; type defaults).
- **Day 300** — Android QA sweep.
- **Day 301** — Week review.

### Week 44 — v2.0 Hardening + **Release** (Days 302–308) (W/B)
- **Day 302** — Indexes + query optimization for show/episode access patterns.
- **Day 303** — Web/admin QA for full series workflow (create → episodes → publish → watch).
- **Day 304** — Security/rules audit (episode-level access + downloads).
- **Day 305** — Cross-platform parity QA (movies + shows + animes).
- **Day 306** — Beta + fixes.
- **Day 307** — Store/web release prep (updated listing: "Now with TV Shows & Anime").
- **Day 308** — **🚀 v2.0 release**; monitoring.

---

# ███ RELEASE v2.1 — MONETIZATION ███  (Days 309–336)
*Subscriptions across Android (Play Billing) and web (FastSpring, merchant-of-record). Free tier preserved; auth/schema already accounted for it.*

### Week 45 — Subscription Model + Entitlements (Days 309–315) (B)
- **Day 309** — Plan design: Free tier vs Premium (HD, offline, early premieres, ad-free); define entitlements/features.
- **Day 310 (B)** — `subscriptions/{userId}` + `entitlements/{userId}` schema; **server-side entitlement resolver** (Firestore = source of truth).
- **Day 311 (B)** — Free-tier gating logic (download cap, quality cap, premiere access) behind feature flags.
- **Day 312 (B)** — Paywall service + "upgrade" surfaces (shared copy/assets across platforms).
- **Day 313 (B)** — Grace period / expiry / cancellation handling; entitlement caching + refresh on clients.
- **Day 314** — Admin: plans/pricing config; comp/grant subscription; entitlement override.
- **Day 315** — Week review.

### Week 46 — Play Billing (Android) (Days 316–322) (A)
- **Day 316** — Google Play Billing integration; products/base plans/offers.
- **Day 317** — Purchase flow + paywall screens; upgrade/downgrade.
- **Day 318** — **Server-side receipt verification** (Play Developer API) → set entitlement.
- **Day 319** — Restore purchases; subscription-management deep link.
- **Day 320** — Real-time Developer Notifications (RTDN) webhook → entitlement updates (renew/cancel/refund).
- **Day 321** — Gating UX on Android (HD/offline/premiere prompts); QA with license-test accounts.
- **Day 322** — Week review.

### Week 47 — FastSpring (Web) (Days 323–329) (W)
- **Day 323** — FastSpring account/store + subscription products; choose popup vs embedded checkout; web paywall surfaces.
- **Day 324** — FastSpring checkout integration on web; success/return + order linking to `userId`.
- **Day 325** — **FastSpring webhooks** → server entitlement updates (order completed, subscription activated/deactivated/charged/canceled); signature verification.
- **Day 326** — Manage-subscription UI (FastSpring account portal link); upgrade/downgrade/cancel; proration.
- **Day 327** — VAT/tax + receipts/invoices (FastSpring handles MoR/tax); display in UI; dunning emails.
- **Day 328** — **Reconcile entitlements across Play + FastSpring** (one user, multiple sources → highest entitlement wins).
- **Day 329** — Web gating UX (HD/offline/premiere prompts); QA in FastSpring test mode; week review.

### Week 48 — Billing Ops + **v2.1 Release** (Days 330–336) (W/B)
- **Day 330** — Billing admin dashboard: MRR, churn, active subs by provider (Play vs FastSpring); refunds.
- **Day 331** — Dunning/recovery (failed-payment retries, reminders); grace UX.
- **Day 332** — Compliance: tax/receipts, refund policy, subscription terms, Play policy review.
- **Day 333** — Security audit (webhook signature verification, entitlement tamper-proofing, replay protection).
- **Day 334** — End-to-end QA across both providers (purchase → entitle → expire → restore/refund).
- **Day 335** — Beta + fixes.
- **Day 336** — **🚀 v2.1 release**; monitoring + revenue dashboards.

---

# ███ v2.2+ — FUTURE BACKLOG ███  (unscheduled)
- **Smart ML recommendations** (embeddings / collaborative filtering) replacing heuristic recs.
- **Public theaters at scale** (sharded sync, thousands of concurrent viewers).
- **Creator / curator program** (verified curators, public collections as a feature).
- **Live events / watch-along with hosts**; "Filmatube Originals" badge.
- **Social graph discovery** ("people with your taste" map), group chats / DMs.
- **Ads-supported tier** (AdMob/web) as an alternative to subscription.
- **iOS app** (the `iOS/` folder exists in the old tree).
- **Localization beyond EN/FR**.

---

## Schema additions by version

**Core (v1.0)** — see `FIRESTORE_SCHEMA.md` (movies-only). `movies` doc includes `videoKey`, optional `introStart`/`introEnd`, `subtitleTracks: {lang,url}[]`, `audioTracks: {lang,url}[]`, `isComingSoon`.

**Theater (v1.2)**:
```
showtimes/{showtimeId}
  movieId, movieTitle, posterUrl, backdropUrl
  startAt Timestamp · status "scheduled"|"lobby"|"live"|"ended"
  isPremiere boolean · capacity number(0=∞) · hostId string
  position number(seconds, server-driven) · attendeesCount number · createdAt
showtimes/{}/attendees/{userId}   { rsvp, joinedAt }
showtimes/{}/chat/{messageId}     { userId, userName, userAvatar, text, isSpoiler, reactions, createdAt }
```

**Recommendations (v1.3)**:
```
recs/{userId}                            { rows: {key,title,contentIds[]}[], updatedAt }
recFeedback/{userId}/items/{contentId}   { signal: "not_interested"|"seen", at }
```

**Referral (v1.3)**:
```
referrals/{referralId}   { referrerId, code, refereeId, status, rewardGranted, createdAt }
users/{userId}           + referralCode, referredBy, referralCount
```

**Kids / Parental (v1.5)** — extend `users/{}/profiles`:
```
isKids boolean · maxAgeRating string · watchTimeLimit number(min/day, optional)
users/{userId}: parentalPin (hashed, account-level)
```

**Series & Anime (v2.0)**:
```
tvshows/{showId}, animes/{animeId}                          (generic content shape + type)
tvshows/{showId}/seasons/{seasonId}
tvshows/{showId}/seasons/{seasonId}/episodes/{episodeId}    (videoKey, introStart/End, recapEnd, airDate, subtitleTracks)
```
Add `type: movie|tvshow|anime` across content + feed/recs/watchProgress.

**Monetization (v2.1)**:
```
subscriptions/{userId}   { provider: "play"|"fastspring", plan, status, currentPeriodEnd, source }
entitlements/{userId}    { tier: "free"|"premium", features[], updatedAt }   (server-resolved)
plans/{planId}           { name, price, currency, features[], providerProductIds }
```
