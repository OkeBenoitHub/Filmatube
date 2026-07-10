# Social Parity QA — Android ↔ Web (Day 98)

Covers **follow graph**, **activity feed**, **reactions**, **recommendations**, **public profiles**,
**taste match**, and **follow suggestions** — closing **Phase 7 (Days 85–98)**. Both clients read/write
the same Firestore paths in project **filmatubelive**, so social state is cross-platform by design.

## Shared data model
| Feature | Firestore path | Android | Web |
|---|---|---|---|
| Follow graph | `follows/{followerId}_{followedId}` | `FollowRepository` (realtime toggle) | `FollowButton` (client toggle) |
| Follower/following lists | query `follows` by `followedId` / `followerId` | `FollowListScreen` | `/u/[id]/followers`, `/u/[id]/following` |
| Activity feed | `feed/{uid}/events/{eventId}` | `FeedScreen` (Community tab) | `/activity` (`ActivityFeed`) |
| Reactions | `reactions/{uid}/items/{movieId}` (`kind: "reaction"`) | `ReactionRepository` + detail bar | `ReactionBar` on `/movie/[id]` |
| Reaction counts | `collectionGroup("items")` where `movieId` + `kind` | `reactionCounts` | `ReactionBar` tally |
| Recommendations | `recommendations/{toUid}/items/{id}` | `RecommendationRepository` + inbox | `RecommendButton` + `/inbox` |
| Public profile | `users/{uid}` (+ derived counts) | `PublicProfileScreen` | `/u/[id]` |
| Taste match | Jaccard of `users/{uid}.genrePreferences` | `SocialRepository.tasteMatch` | `lib/social.tasteMatch` |
| Suggestions | scan `users`, rank by genre overlap | `SocialRepository.suggestedUsers` | `lib/social.getSuggestedUsers` → `/discover` |

- **Counts are derived by query** (rules block cross-user count writes on `users/{uid}`), identical on both.
- **Feed fan-out is client-side**: the actor writes one event into each follower's `feed/{uid}/events`
  inbox (a Cloud Function can replace this later without changing readers).

## Field parity (write shape)
- **Follow doc**: `{ followerId, followedId, createdAt }`, id = `{followerId}_{followedId}`.
- **Recommendation**: `{ fromUserId, fromName, fromAvatar, movieId, movieTitle, message, createdAt }`.
- **Reaction**: `{ movieId, type, kind: "reaction", updatedAt }` (types: `love`/`fire`/`mind_blown`/`boring`).
- **Feed event**: `{ actorId, actorName, actorAvatar, type, movieId, movieTitle, createdAt }`
  (types: `watching`/`watched`/`added_watchlist`/`liked`/`reacted`/`added_collection`).

## Bidirectional sync (verify with one account on both platforms)
- [ ] **Follow — Android → Web:** follow a user on Android → web `/u/{them}` shows *Following* and the
      follower count increments; they appear in your `/u/{me}/following` list.
- [ ] **Follow — Web → Android:** follow on web → Android profile/follow list reflects it (realtime).
- [ ] **Unfollow** on either platform clears it on both.
- [ ] **Activity feed:** an action on one platform (start playback, add to Watch Later, react) appears in
      **followers'** `/activity` feed on the other platform, newest first; **Today/Week/All** filters work.
- [ ] **Reactions:** react on Android detail → web `/movie/{id}` shows your reaction highlighted and the
      count includes it (and vice-versa) — **requires the `items(movieId, kind)` index deployed**.
- [ ] **Recommendation:** recommend a movie to someone you follow on web → it lands in their Android
      inbox (and web `/inbox`) with your name, the movie, and message.
- [ ] **Taste Match %** on `/u/{id}` and `/discover` matches the Android badge for the same pair of users.

## Web-only conveniences (not parity gaps)
- `/discover` "People with your taste" is reachable from the Activity header and empty-state CTA.
- Public profiles surface the user's **public collections** (collections are a web authoring surface today).
- Per-actor **mute** on the web feed is stored in `localStorage` (`filmatube.mutedActors`); Android mute is
  in DataStore. Mute is intentionally device-local on both.

## Known infra gaps (both platforms, tracked)
- **FCM push** for reactions/recommendations needs a Cloud Function; the in-app feed/inbox work today.
- **Reaction counts** need `firebase deploy --only firestore:indexes` (the `items(movieId, kind)`
  collection-group index) — until then counts degrade to 0 (web wraps the query in try/catch; no crash).
- Feed/notification **fan-out is client-side** (a Function can centralize it later).

## Status
Phase 7 complete (Days 85–98). Android social shipped Days 85–91; Web social Days 92–98.
Both builds green — Android `assembleDebug`, Web `next build`. Runtime rows above are for manual
cross-device verification with a single account signed in on both platforms.
