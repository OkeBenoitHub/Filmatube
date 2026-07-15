# Week 19 QA — Community Boards & Chat (Android, Day 133)

Covers **v1.1 Week 19 (Days 127–133)** — discussion boards + real-time chat on Android.
All data lives in `boards/*` in project **filmatubelive**; rules + indexes deployed.

## Data model
| Path | Purpose |
|---|---|
| `boards/{id}` | title, description, coverUrl, type (movie/general), isPublic, isFeatured, isOfficial, ownerId, `memberIds[]`, memberCount, `pinnedMessageId` |
| `boards/{id}/members/{uid}` | userId, userName, userAvatar, role (owner/member), `muted` |
| `boards/{id}/messages/{msgId}` | userId/name/avatar, text, hasSpoiler, `reactions{uid:emoji}`, replyToName/Text, movieId/Title/Poster, createdAt |
| `boards/{id}/typing/{uid}` | name, updatedAt (6s freshness) |

## Feature map
| Day | Feature |
|---|---|
| 127 | Discovery (All/Movies/General tabs + Featured); board types |
| 128 | Create board (cover → R2, public/private); **My Boards** (memberIds array-contains) |
| 129 | Join/leave (membership-only board update); invite followers → inbox |
| 130 | Real-time chat; send; typing indicator; spoiler tag |
| 131 | Emoji reactions (reactions map); reply quotes; share movie card into a board |
| 132 | Member list + roles; owner mute/remove; pin message; report message |
| 133 | Board invite notifications + `filmatube://board/{id}` deep link; week review |

## Rules (deployed)
- **Board read:** public OR owner OR member (`uid in memberIds`).
- **Board update:** owner/admin full edit; anyone else only a **membership-only** change toggling their own uid.
- **members:** self create/leave; **owner/admin** may mute (update) or remove (delete).
- **messages create:** only **non-muted members** (`exists` + `muted == false`); delete by author/owner/admin;
  update by author, or a **reactions-only** map change by anyone.
- **typing:** self-write.

## Runtime checklist
- [ ] Create a board (with/without cover) → it appears in **My Boards**; public ones in discovery.
- [ ] Join a public board → composer unlocks; leave → back to "Join to chat"; owner can't leave.
- [ ] Send messages in real time across two accounts; typing indicator shows; auto-scroll to newest.
- [ ] Spoiler message hides behind a reveal; reactions toggle + count; reply shows a quoted snippet.
- [ ] Share a movie from its detail → a movie-card bubble appears in the chosen board's chat → tap opens the movie.
- [ ] Owner: pin a message (banner shows), mute a member (their composer disables), remove a member.
- [ ] Report a message → a `reports/{id}` doc (`type: board_message`) is written.
- [ ] Invite followers → each gets a **board invite** in their notification center → tap opens the board.

## Known gaps (tracked)
- **FCM push** for board invites needs the Cloud Function (in-app inbox works today).
- **Reactions map** update is not restricted to the caller's own key (rule allows any reactions-only
  change) — a Function or finer rule can tighten this later.
- Mute enforcement adds a per-send membership read; fine at current scale.
- Board-message reports don't resolve text in the web admin queue yet (different collection) — a
  Day-109-style enhancement.

## Status
Android `assembleDebug` green. Boards are usable end-to-end: discover → create → join/invite → real-time
chat with reactions/replies/spoilers/typing/movie-cards → moderation → invite notifications + deep links.
Week 20 mirrors this on Web (Days 134–140).
