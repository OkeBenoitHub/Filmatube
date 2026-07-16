# Boards QA — Community Boards & Chat (Android + Web)

Covers **v1.1 Week 19 (Days 127–133, Android)** and **Week 20 (Days 134–140, Web)**.
Both clients read and write the same `boards/*` documents in project **filmatubelive**, so a
message sent on one platform appears live on the other. Rules + indexes deployed.

## Data model
| Path | Purpose |
|---|---|
| `boards/{id}` | title, description, coverUrl, type (movie/general), isPublic, isFeatured, isOfficial, ownerId, `memberIds[]`, memberCount, `pinnedMessageId` |
| `boards/{id}/members/{uid}` | userId, role (owner/member), joinedAt, `muted` |
| `boards/{id}/messages/{msgId}` | userId/name/avatar, text, hasSpoiler, `reactions{uid:emoji}`, replyToName/Text, movieId/Title/Poster, createdAt |
| `boards/{id}/typing/{uid}` | name, updatedAt (6s freshness) |
| `users/{uid}/notifications/*` | `board_invite` carries boardId + boardTitle |
| `reports/{id}` | `type: board_message` carries boardId + targetId |

Member docs hold only uid/role/muted — display names and avatars are joined from `users/{uid}`.

## Feature map
| Day | Android (W19) | Day | Web (W20) |
|---|---|---|---|
| 127 | Discovery (All/Movies/General + Featured) | 134 | Discovery + create (hero, chips, grids) |
| 128 | Create board; My Boards | 135 | Real-time chat; reactions/threads |
| 129 | Join/leave; invite followers | 136 | Roles + moderation; admin official boards |
| 130 | Real-time chat; typing; spoiler | 137 | Share movie card; spoilers; board search |
| 131 | Reactions; replies; movie cards | 138 | Board moderation queue; report handling |
| 132 | Members + roles + moderation; report | 139 | Parity QA (Android↔web) |
| 133 | Invite notifications + deep links | 140 | Week review |

## Rules (deployed)
- **Board read:** public OR owner OR member (`uid in memberIds`).
- **Board update:** owner/admin full edit; anyone else only a **membership-only** change
  (`hasOnly(['memberIds','memberCount'])`) toggling their own uid.
- **members:** self create/leave; **owner/admin** may mute (update) or remove (delete).
- **messages create:** only **non-muted members** (`exists` + `muted == false`); delete by
  author/owner/admin; update by author, or a **reactions-only** map change by anyone.
- **typing:** self-write.
- **notifications create:** `actorId == auth.uid && type != 'system'` — covers `board_invite`.

## Parity notes (Day 139 audit)
Five divergences were found and fixed; these are the ones to re-check if either client changes:
1. **Message window** — must be `createdAt desc` + limit + reverse on both. An ascending limit
   pins a busy board to its oldest messages.
2. **Author identity** — name/avatar come from `users/{uid}`, never the Firebase Auth profile.
   The web never syncs profile edits back to Auth (`photoURL` is always empty), so Auth is wrong.
3. **replyToText** — capped at 120 chars on both.
4. **Notification targets** — board first, then movie, then actor.
5. **Invite followers** — `follows` where `followedId == me` → `followerId`.

## Runtime checklist
- [ ] Create a board (with/without cover) on either client → appears in **My Boards**; public ones in discovery.
- [ ] Join a public board → composer unlocks; leave → back to the join gate; owner can't leave.
- [ ] **Cross-platform:** send from Android, see it on web live (and back) with the same name/avatar.
- [ ] Typing indicator appears within ~3s and clears after 6s; chat auto-scrolls to newest.
- [ ] Spoiler message hides behind a reveal on both; reactions toggle + count; reply shows a quoted snippet.
- [ ] Share a movie → a movie-card bubble appears in the chosen board → tap/click opens the movie.
- [ ] Owner: pin (banner shows on both), mute a member (their composer disables), remove a member.
- [ ] Report a message → `reports/{id}` (`type: board_message`) → visible under **/admin/reports?scope=boards**
      with its text resolved; delete / mute-in-board / remove-from-board all work.
- [ ] Admin: mark a board official/featured in **/admin/boards** → badge + featured row update.
- [ ] Invite followers → each gets a **board invite** → tap opens the board (Android also via
      `filmatube://board/{id}`).
- [ ] Board search matches title/description/movie; the type chips preserve the query.

## Known gaps (tracked)
- **FCM push** for board invites still needs a Cloud Function (the in-app inbox works on both).
- **Reactions map** updates aren't restricted to the caller's own key (the rule allows any
  reactions-only change) — a Function or finer rule can tighten this.
- Mute enforcement costs a membership read per send; fine at current scale.
- **Board search** filters the fetched page (≤200 public boards) in memory — Firestore has no
  substring search. Needs a search index if the catalog of boards grows.
- Web has no board deep-link scheme equivalent; it uses plain `/boards/{id}` URLs.

## Status
Android `assembleDebug` and web `npm run build` both green. Boards are end-to-end on **both**
platforms: discover/search → create → join/invite → real-time chat with reactions, replies,
spoilers, typing and movie cards → owner moderation → admin official boards + report queue.
Week 21 begins Android Watch Parties (Days 141–147).
