# Filmatube — Firestore Schema (movies-only, v1.0)

All timestamps are Firestore `Timestamp`. IDs are auto-IDs unless noted. Binaries (video,
posters, avatars, subtitles) live in **Cloudflare R2** — Firestore stores only keys/URLs.
TV shows & animes are deferred to v2.0 (content model keeps a `type` field so they slot in).

---

## `users/{userId}`  (userId = Firebase Auth UID)
```
email           string
displayName     string
bio             string
avatarUrl       string    (R2 public URL)
language        string    ("en" | "fr")
followersCount  number
followingCount  number
isAdmin         boolean   (mirrored by the `admin` custom claim — claim is authoritative)
isBanned        boolean
createdAt       Timestamp
lastActiveAt    Timestamp
```

### `users/{userId}/profiles/{profileId}` — Netflix-style watch profiles
```
name             string
avatarEmoji      string
isDefault        boolean
language         string   ("en" | "fr")
genrePreferences string[]
createdAt        Timestamp
```
*(v1.5 adds: `isKids`, `maxAgeRating`, `watchTimeLimit`.)*

### `users/{userId}/notifications/{notificationId}`
```
type            string    ("follow" | "like" | "reaction" | "recommendation" |
                           "new_movie" | "coming_soon" | "request_update")
fromUserId      string?
fromUserName    string?
fromUserAvatar  string?
movieId         string?
movieTitle      string?
message         string?
isRead          boolean
createdAt       Timestamp
```

---

## `movies/{movieId}`
```
type            string    ("movie" — future: "tvshow" | "anime")
imdbId          string
tmdbId          string
title           { en: string, fr: string }
description     { en: string, fr: string }
posterUrl       string    (R2 public URL)
backdropUrl     string
thumbnailUrl    string?
trailerUrl      string?   (YouTube)
videoKey        string    (R2 object key in the PRIVATE videos bucket — played via
                           short-lived presigned URLs from /api/stream, never a public URL)
subtitleTracks  { lang: string, url: string }[]   (R2 public .vtt URLs)
audioTracks     { lang: string, url: string }[]?
introStart      number?   (seconds — skip-intro marker)
introEnd        number?
genres          string[]
year            number
duration        number    (minutes)
ageRating       string    ("G" | "PG" | "PG-13" | "R" | "NC-17")
cast            { name: string, character: string, photoUrl: string }[]
directors       string[]
language        string    ("en" | "fr" | "both")
averageRating   number    (denormalized from reviews)
ratingsCount    number
likesCount      number
viewsCount      number
status          string    ("draft" | "published")
isFeatured      boolean
isPinned        boolean
isComingSoon    boolean
addedAt         Timestamp
updatedAt       Timestamp
```

---

## `watchProgress/{userId}/items/{movieId}`
```
position    number   (seconds)
duration    number   (seconds)
isWatched   boolean  (true at >= 90%)
updatedAt   Timestamp
```

## `watchlists/{userId}/movies/{movieId}`
```
addedAt     Timestamp
```

## `likes/{userId}/items/{movieId}`
```
createdAt   Timestamp
```

## `reactions/{userId}/items/{movieId}`
```
reaction    string   ("love" | "fire" | "mind_blown" | "boring")
updatedAt   Timestamp
```

---

## `collections/{collectionId}` — user-curated lists
```
userId      string
title       string
description string
coverUrl    string   (R2)
isPublic    boolean
itemCount   number
createdAt   Timestamp
updatedAt   Timestamp
```
### `collections/{collectionId}/items/{movieId}`
```
addedAt     Timestamp
```

---

## `follows/{followId}`  (followId = `{followerId}_{followedId}`)
```
followerId  string
followedId  string
createdAt   Timestamp
```

## `feed/{userId}/events/{eventId}` — activity feed (fan-out to followers)
```
actorId       string
actorName     string
actorAvatar   string
type          string   ("watching" | "watched" | "liked" | "reacted" |
                        "added_watchlist" | "added_collection")
movieId       string
movieTitle    string
moviePoster   string
reaction      string?
createdAt     Timestamp
expiresAt     Timestamp   (createdAt + 30d, for cleanup)
```

## `recommendations/{toUserId}/items/{recommendationId}`
```
fromUserId      string
fromUserName    string
fromUserAvatar  string
movieId         string
movieTitle      string
moviePoster     string
message         string
isRead          boolean
createdAt       Timestamp
```

---

## `reviews/{movieId}/items/{reviewId}`
```
userId      string
userName    string
userAvatar  string
rating      number   (1–5)
text        string
isSpoiler   boolean
likesCount  number
createdAt   Timestamp
updatedAt   Timestamp
```

## `comments/{movieId}/items/{commentId}`
```
userId      string
userName    string
userAvatar  string
text        string
isSpoiler   boolean
parentId    string?  (null = top-level)
likesCount  number
createdAt   Timestamp
```

---

## `requests/{requestId}` — user content requests
```
userId      string
userName    string
title       string
year        number?
imdbId      string?
note        string
status      string   ("pending" | "approved" | "rejected")
adminNote   string?
createdAt   Timestamp
updatedAt   Timestamp
```

---

## `achievements/{userId}/badges/{badgeId}`
```
badgeId     string   ("first_watch" | "binge_watcher" | "cinephile" | "critic" |
                      "social_butterfly" | "premiere_goer" | "recruiter")
unlockedAt  Timestamp
```

## `stats/{userId}`
```
totalWatchMinutes  number
moviesCompleted    number
reviewsWritten     number
topGenres          string[]
updatedAt          Timestamp
```

---

## `showtimes/{showtimeId}` — Online Movie Theater (v1.2; read by the Theater tab since Day 155)
```
movieId         string
movieTitle      string
posterUrl       string
backdropUrl     string
startAt         Timestamp
status          string   ("scheduled" | "lobby" | "live" | "ended")
isPremiere      boolean
capacity        number   (0 = unlimited)
hostId          string?
position        number   (seconds — server/host driven)
attendeesCount  number
createdAt       Timestamp
```
Also written by the admin CMS / automation:
```
durationMs      number    (denormalized movie runtime — how the automation knows when to end)
boardId         string    ("" = public; otherwise private to that board)
waitlistCount   number
presentCount    number    (server-maintained; see presence below)
recurrence      string    ("none" | "daily" | "weekly")
recurrenceSpawned boolean (guard: the next occurrence has already been queued)
endedAt         Timestamp
remindSentAt    Timestamp (guard: the "starting soon" reminder has already gone out)
```

`attendeesCount` is maintained by the `syncShowtimeAttendees` Cloud Function, not by clients —
showtime docs stay admin-writable, unlike boards where members bump their own `memberCount`.

**Lifecycle** is driven by `processTheaterSchedule` (every minute): `scheduled` → `lobby` at
`startAt - 15min` → `live` at `startAt` → `ended` at `startAt + durationMs`. Because an admin
pause shifts `startAt` forward, the end time moves with it and needs no separate bookkeeping.
A late status flip is cosmetic — clients derive position from `startAt`, not from the flip.

### `showtimes/{showtimeId}/attendees/{userId}` → `{ rsvp, remind, joinedAt }`
### `showtimes/{showtimeId}/chat/{messageId}` → `{ userId, userName, userAvatar, text, isSpoiler, createdAt }`
### `showtimes/{showtimeId}/reactions/{reactionId}` → `{ userId, userName, emoji, createdAt }`
### `showtimes/{showtimeId}/presence/{userId}` → `{ userId, presentAt }`
Heartbeat while actually watching (~30s), stale after 90s. Deliberately **not** the attendees
doc: an RSVP is intent, presence is fact, and merging them would let walking into a room
inflate the `attendeesCount` shown before it starts.

Clients read the denormalized `presentCount` rather than subscribing to this subcollection.
With N viewers all heartbeating, a collection listener delivers N documents to N listeners —
O(N²), roughly a million reads per 30s at a thousand viewers. `syncShowtimePresence` maintains
the counter on arrival/departure, and `processTheaterSchedule` sweeps records whose heartbeat
stopped (an app killed without a clean exit), whose deletes come back as decrements.

### `showtimes/{showtimeId}/waitlist/{userId}` → `{ userId, joinedAt }`
Queue for a seat once the room is full. `promoteFromWaitlist` pulls the longest-waiting person
in when an attendee leaves, claiming the place by deleting the entry *first* so two concurrent
departures can't promote the same person twice.

**Private screenings** set `boardId`; only that board's members may read or join. The read rule
means a list query must constrain `boardId` itself — a query that *could* return a private
showtime fails outright rather than filtering it out — so both clients query with `boardId == ""`.
### `showtimes/{showtimeId}/friendNotified/{userId}` → `{ at }`
Function-only marker so "a friend is in a theater" fires once per viewer per showing.
`showtimes.remindSentAt` is the matching guard for the "starting soon" reminder.

Playback position is **not** stored: a showing runs on the wall clock, so every viewer derives
its position as `(pausedAt ?? serverNow) - startAt`. The `position` field above is reserved for
the Day 170 automation and is unused by the clients.

`pausedAt` (Timestamp?, admin-written) is how the Day 167 host controls work without a host:
- **Pause** — set `pausedAt = now`; the effective clock freezes and every viewer holds.
- **Resume** — `startAt += (now - pausedAt)`, clear `pausedAt`; the room resumes exactly where
  it froze rather than jumping ahead by the length of the intermission.
- **Skip ±N** — `startAt ∓= N`; position and start time move in opposite directions.

Because position stays a pure function of `startAt`/`pausedAt`, there is no accumulated pause
bookkeeping to drift out of step, and one admin write steers the whole room with no per-viewer
traffic. Both clients implement the same formula (`Showtime.playbackPositionMs`).

---

## `recs/{userId}` — personalized recommendations (v1.3, Day 183)
```
topPicks    string[]   (movieIds — the overall "For you" row, best matches first)
rows        [{ seedMovieId, seedTitle, seedPoster, movieIds: string[] }]
                       ("Because you watched X" rails, strongest seed first)
generatedAt Timestamp
```
Written **only** by the scheduled `buildRecommendations` Cloud Function (every 24h) — never
by clients. Content-overlap scoring, **no ML**: a taste profile is accumulated from the
signals a user already leaves (finished a movie, liked it, reacted love/fire/mind_blown, or
watchlisted it), weighted per signal, and spread over each seed movie's genres, cast and
directors. Every published movie the user hasn't already engaged with is then scored by how
much its genres/people overlap that profile (people weighted above genre); anything they
finished, saved, disliked (`boring`) or dismissed is excluded. Reasons are carried as the
seed movie, so the client can say *why* without recomputing.

## `recFeedback/{userId}/items/{movieId}` — "not interested" (v1.3)
```
action      string   ("dismissed")
createdAt   Timestamp
```
Self-written. The next `buildRecommendations` run reads these and excludes the movie, so a
dismissal sticks rather than reappearing on the next rebuild.

## Later versions (documented for planning, not yet enforced)
- **v1.3:** `referrals/{referralId}` (Days 197+)
- **v2.0:** `tvshows/*`, `animes/*` + `seasons`/`episodes` subcollections
- **v2.1:** `subscriptions/{userId}`, `entitlements/{userId}`, `plans/{planId}`
