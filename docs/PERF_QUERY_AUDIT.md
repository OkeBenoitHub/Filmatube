# Firestore Query & Pagination Audit — Android (Day 113)

Audit of every Android Firestore read path: index coverage, result caps, and pagination.
Project **filmatubelive**. Indexes live in `firestore.indexes.json` (all deployed).

## Movie catalog (`MovieRepositoryImpl`)
| Method | Query | Composite index | Cap |
|---|---|---|---|
| `getFeatured` | status== + isFeatured== + order addedAt desc | `movies(status, isFeatured, addedAt)` ✓ | `limit` |
| `getTrending` | status== + order viewsCount desc | `movies(status, viewsCount)` ✓ | `limit` |
| `getNewReleases` | status== + order addedAt desc | `movies(status, addedAt)` ✓ | `limit` |
| `getComingSoon` | status== + isComingSoon== + order addedAt desc | `movies(status, isComingSoon, addedAt)` ✓ | `limit` |
| `getByGenre` / `getRelated` | genres array-contains + status== + order addedAt | `movies(genres, status, addedAt)` ✓ | `limit` |
| `browse` | status== + order addedAt \| averageRating, filter client-side | `movies(status, addedAt)`, `movies(status, averageRating)` ✓ | **200** |
| `search` / `getByActor` | status== + order addedAt, filter client-side | `movies(status, addedAt)` ✓ | **200** |

- **All movie paths are single index-backed reads.** Genre/year/text filters run client-side on a
  capped 200-doc window (Firestore has no substring search) — correct and cheap at catalog scale.
- Home rows each fetch only their `limit` (≈10–20). Detail fetches one doc.

## Social & viewing state
| Path | Bound |
|---|---|
| `follows` where follower/followedId | index `follows(followerId, createdAt)` / `(followedId, createdAt)` ✓ |
| `feed/{uid}/events` | ordered + **paginated** (`limit`, Load-more) |
| `reactions` counts | `collectionGroup(items)` where movieId+kind — index `items(movieId, kind)` ✓ |
| `recommendations/{uid}/items` | ordered + `limit(50)` |
| `watchlists` / `watchProgress` | per-user subtree, small |
| `requests` where userId + order createdAt | index `requests(userId, createdAt)` ✓ |
| `users/{uid}/notifications` | ordered + `limit(50)` |

## Fixes applied this pass
- **`reviews/{movieId}/items`** realtime observer was unbounded → now `limit(100)`
  (`ReviewRepository.MAX_REVIEWS`).
- **`comments/{movieId}/items`** realtime observer was unbounded → now `limit(200)`
  (`CommentRepository.MAX_COMMENTS`).
  Both bound worst-case reads on very popular titles while keeping realtime updates.

## Known scale notes (acceptable for current stage)
- **Follower/following counts** derive from reading the `follows` collection (rules block
  cross-user count writes). Fine for expected list sizes; when a user can exceed ~1k follows,
  move counts to a Cloud Function–maintained field.
- **`browse`/`search`** read a 200-doc window. When the published catalog grows past a few hundred,
  switch to cursor pagination (`startAfter(lastSnapshot)`) on the ordered query; the client-side
  filters would then page incrementally. Not needed at current catalog size.

## Status
All Android query paths are index-backed and result-capped; realtime observers are now bounded.
No missing composite indexes. Android `assembleDebug` green.
