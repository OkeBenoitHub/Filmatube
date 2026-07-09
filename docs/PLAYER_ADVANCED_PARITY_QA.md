# Advanced Player Parity QA + Phase 5 Review (Day 70)

Closes **Phase 5 — Playback Advanced (Days 57–70)**. Covers the advanced player features on
both platforms and their cross-device behaviour. Complements the earlier player QA docs
(`PLAYER_PARITY_QA.md` Day 53, `CROSS_DEVICE_PLAYBACK_QA.md` Day 56).

## What shipped in Phase 5
**Android (Week 9, Days 57–63)**
- Subtitles (.vtt from R2) + language selector; subtitle **style** (size/color/background/edge/
  position) persisted via DataStore.
- Audio-track selector (multi-language) + playback speed (0.5×–2×).
- Sleep timer (15/30/45/60 + end-of-movie) with countdown badge + auto-pause.
- "Up Next" autoplay (last-30s card + countdown) + skip-intro (admin markers).
- Audio focus + audio-becoming-noisy; network-loss auto-resume; gesture refinement.
- Feature analytics (`video_feature`).

**Web (Week 10, Days 64–70)**
- Settings gear: speed, quality placeholder, captions toggle.
- Subtitles via same-origin `/api/subtitle` proxy; caption **style** (size/color/background)
  persisted in localStorage; audio-track switching (`video.audioTracks`).
- Sleep timer + Up Next autoplay.
- Admin: audio-track + intro-marker editors; R2 orphan-cleanup tool.
- Network resilience (retry + auto-retry on reconnect) + analytics.

## Parity (by design)
| Feature | Android | Web |
|---|---|---|
| Subtitles | Media3 `SubtitleConfiguration` from the movie's `subtitleTracks` | `<track>` from the same `subtitleTracks` (proxied) |
| Caption style | CaptionStyleCompat, DataStore-persisted | `::cue` CSS, localStorage-persisted |
| Audio tracks | ExoPlayer `onTracksChanged` (embedded) | `video.audioTracks` (embedded) |
| Playback speed | `player.setPlaybackSpeed` | `video.playbackRate` |
| Sleep timer | 1s tick → pause; badge | 1s tick → pause; badge |
| Up Next | related → new-release; autoplay on end | same source; autoplay on end |
| Skip-intro | reads `introStart`/`introEnd` markers | markers editable in CMS (web player skip UI later) |
| Progress | `watchProgress/{uid}/items/{movieId}` | same document |
| Analytics | `video_play/pause/complete` + `video_feature` | identical event names + params |

Both read the **same movie fields** the CMS writes: `subtitleTracks[{lang,url}]`, `audioTracks
[{lang,label}]`, `introStart`/`introEnd`.

## Manual checklist
- [ ] **Subtitles:** a movie with EN + FR `.vtt` shows a captions picker on both; selecting a
      language renders cues; Off hides them.
- [ ] **Caption style:** change size/color/background — persists across movies and restart
      (DataStore on Android, localStorage on web).
- [ ] **Audio:** a multi-audio movie shows the audio picker on both; single-audio hides it.
- [ ] **Speed:** 0.5×–2× applies immediately on both.
- [ ] **Sleep timer:** set 15 min → countdown badge on both → auto-pauses at 0; "end of movie"
      does not trigger Up Next autoplay.
- [ ] **Up Next:** last-30s card + countdown → autoplays the next movie; Dismiss cancels.
- [ ] **Skip-intro:** with `introStart`/`introEnd` set in the CMS, Android shows Skip Intro in range.
- [ ] **Progress + analytics:** cross-device resume still works (per Day 56); analytics events
      appear in Firebase for both platforms.

## Expected differences (not bugs)
- Caption styling is richer on Android (edge/position) than web (`::cue` is limited).
- Embedded audio-track switching is broadly supported on Android; on web it is effectively
  Safari-only (`video.audioTracks`), so the picker rarely appears in Chromium.
- Skip-intro UI is on the Android player now; the web player's skip button is a later polish item.

## Status
Both builds green as of Week 10 close — Android `assembleDebug`, Web `next build` (28 routes).
**Phase 5 (Playback Advanced) complete.** Runtime checklist is for manual verification once
movies with uploaded video + subtitle/audio tracks exist.
