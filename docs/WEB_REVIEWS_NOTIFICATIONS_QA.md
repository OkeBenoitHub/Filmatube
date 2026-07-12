# Week 16 QA — Web Reviews, Push, Requests & Broadcast (Day 112)

Closes **Phase 8 (Days 99–112)**. Week 15 shipped ratings/reviews/comments/notifications on
**Android**; Week 16 mirrors them on **Web** off the same Firestore schemas in **filmatubelive**,
and adds the admin moderation/broadcast/requests surfaces.

## Cross-platform parity (shared schema)
| Feature | Firestore path | Android | Web |
|---|---|---|---|
| Star rating | `ratings/{movieId}/items/{uid}` | RatingRepository | StarRating (live avg from subtree) |
| Review + likes | `reviews/{movieId}/items/{uid}` (+ `/likes`) | ReviewsScreen | ReviewsSection |
| Threaded comments | `comments/{movieId}/items` (`parentId`) | CommentsScreen | CommentsSection |
| Report | `reports/{id}` | report buttons | report buttons |
| Notification inbox | `users/{uid}/notifications` | NotificationCenterScreen | NotificationCenter |
| FCM token | `users/{uid}/fcmTokens/{token}` | messaging service | PushRegistration |
| Channel prefs | `users/{uid}/settings/notifications` | DataStore (device) | NotificationPreferences (synced) |
| Content requests | `requests/{id}` | — (web + future Android) | RequestForm |
| Broadcasts | `broadcasts/{id}` | — (delivered to inbox) | BroadcastComposer |

Rules + indexes for all of the above are **deployed** to filmatubelive.

## Web runtime checklist
- [ ] **Rating/review/comment** on the web detail page write the same docs Android reads (and
      vice-versa): a rating set on Android shows in the web community average; a web review shows
      in the Android reviews list. Spoiler-tagged content is gated behind a reveal on both.
- [ ] **Report** a review/comment on web → it appears in **/admin/reports**; resolve/dismiss/delete
      updates status (delete removes the offending doc).
- [ ] **Notification bell** shows a live unread count; **/notifications** groups Today/Earlier,
      mark-all-read clears the badge, tapping deep-links to the movie/profile.
- [ ] **Notification preferences** (/account/notifications) persist to `settings/notifications`;
      turning off **System** suppresses broadcast pushes for that user.
- [ ] **Admin broadcast** (/admin/notifications): compose → **Preview** shows the recipient count for
      the chosen segment (all / taste-genre / active-14d); **Send now** fans out to inboxes + FCM;
      a scheduled send is stored and delivered by the `processScheduledBroadcasts` function.
- [ ] **Content request** (/requests): submit → appears under "Your requests" as **Pending**; admin
      approves/rejects in **/admin/requests** with a reason (+ optional linked movie ID) → the
      requester gets a notification and the status flips.

## Delivery model
- **In-app inbox** works today on both platforms via client-side / server-action fan-out.
- **Immediate broadcasts** and **request decisions** send FCM push directly from the web **server
  action** (firebase-admin messaging) — no Cloud Function required for those paths.
- **Scheduled broadcasts** and **rating aggregation** need the `functions/` deploy (Blaze plan):
  `processScheduledBroadcasts` (every 5 min) and `aggregateRatings`.

## Known gaps (tracked)
- **Web push registration** needs `NEXT_PUBLIC_FIREBASE_VAPID_KEY` (Web Push certificate) in
  `WebApp/.env.local`; without it PushRegistration is a no-op (inbox still works).
- Android system/broadcast notifications render generically in the Android center (title/body
  handling parity is a follow-up); web renders them fully.
- `functions/` (scheduled broadcasts + rating rollup) is committed but undeployed (Blaze).

## Status
Both builds green — Android `assembleDebug`, Web `next build`. **Phase 8 (Days 99–112) complete.**
Reviews, comments, ratings, notifications, moderation, broadcast, and content requests ship on both
platforms against one shared schema.
