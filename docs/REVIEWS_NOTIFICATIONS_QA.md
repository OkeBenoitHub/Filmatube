# Week 15 QA — Ratings, Reviews, Comments & Notifications (Android, Day 105)

Covers **Phase 8 Week 15 (Days 99–105)** on Android: star ratings, written reviews, threaded
comments, moderation reports, spoiler-free mode, and the FCM + in-app notification stack. All
features use Firestore in project **filmatubelive** and share schemas the Web build mirrors in
Week 16 (Days 106–112).

## Data model
| Feature | Firestore path | Notes |
|---|---|---|
| Star rating | `ratings/{movieId}/items/{uid}` `{value 1-5}` | live avg/count read from subtree; Cloud Function denormalizes to `movies/{id}` |
| Review | `reviews/{movieId}/items/{uid}` | one editable review per user; `hasSpoiler` flag |
| Review like | `reviews/{movieId}/items/{reviewId}/likes/{uid}` | count read from subtree |
| Comment | `comments/{movieId}/items/{id}` `{parentId?}` | 1-level threading |
| Comment like | `comments/{movieId}/items/{id}/likes/{uid}` | count read from subtree |
| Report | `reports/{id}` | user-create, admin-read (queue = Day 109) |
| FCM token | `users/{uid}/fcmTokens/{token}` | per device, self-write |
| Notification inbox | `users/{uid}/notifications/{id}` | client-side fan-out; realtime |

Rules for all of the above are deployed (2026-07-10).

## Runtime checklist
- [ ] **Rating:** tap 1–5 stars on a movie → your rating persists (re-tap same star clears it); the
      community average + count update live from other users' ratings.
- [ ] **Review:** write a review (optionally mark **Contains spoilers**), see it in the list; edit
      overwrites; delete removes it. Like another user's review → count increments.
- [ ] **Comment thread:** post a top-level comment; **Reply** to it nests one level; like a comment;
      delete your own; **Report** someone else's (button disables after reporting).
- [ ] **Spoiler-free mode** (Settings): ON → spoiler-tagged reviews/comments show a "tap to reveal"
      gate; OFF → shown inline with a small red **Spoiler** label.
- [ ] **Notifications wired:** following someone, recommending a movie, replying to a comment, and
      liking a review each drop a notification into the recipient's inbox.
- [ ] **Notification center** (Profile bell): unread badge count; Today/Earlier grouping; tap opens
      the movie or the actor's profile and marks read; **Mark all read** clears the badge.
- [ ] **Notification preferences** (Settings → Notifications): toggling a channel off suppresses that
      category of push; POST_NOTIFICATIONS is requested on first launch (Android 13+).

## Known gaps (tracked)
- **Real push delivery needs a Cloud Function** that reads `users/{uid}/fcmTokens` and sends via FCM
  on inbox writes (planned Day 108/110). Today the in-app inbox is populated by client-side fan-out;
  the messaging service handles/route pushes correctly when they arrive.
- **Rating aggregation Cloud Function** (`functions/aggregateRatings`) is committed but undeployed
  (needs the Blaze plan). Until deployed, `movies/{id}.averageRating` is not auto-updated — the
  detail screen shows the live subtree average instead, so users still see correct numbers.
- Reports have no in-app admin UI yet (admin moderation queue = Day 109, web).

## Status
Both builds green — Android `assembleDebug`. Week 15 (Days 99–105) closes the Android half of
Phase 8. Web reviews/comments/push + admin moderation + broadcast + requests come in Week 16.
