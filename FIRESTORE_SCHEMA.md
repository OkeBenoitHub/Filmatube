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

## `showtimes/{showtimeId}` — Online Movie Theater (built in v1.2; schema reserved now)
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
### `showtimes/{showtimeId}/attendees/{userId}` → `{ rsvp, joinedAt }`
### `showtimes/{showtimeId}/chat/{messageId}` → `{ userId, userName, userAvatar, text, isSpoiler, reactions, createdAt }`

---

## Later versions (documented for planning, not yet enforced)
- **v1.3:** `recs/{userId}`, `recFeedback/{userId}/items`, `referrals/{referralId}`
- **v2.0:** `tvshows/*`, `animes/*` + `seasons`/`episodes` subcollections
- **v2.1:** `subscriptions/{userId}`, `entitlements/{userId}`, `plans/{planId}`
