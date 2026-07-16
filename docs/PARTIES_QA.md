# QA — Watch Parties (Android + Web)

Covers **v1.1 Week 21 (Days 141–147, Android)** and **Week 22 (Days 148–151, Web)** — private,
host-synced watch parties. Both clients read/write the same `parties/*` documents in project
**filmatubelive** (eur3), with the **same engine constants**, so a room can mix platforms: an
Android host can drive a web guest and vice-versa. Rules + index deployed.

## Data model
| Path | Purpose |
|---|---|
| `parties/{id}` | movieId/Title/Poster, hostId/hostName, status (scheduled/live/ended), scheduledAt, `memberIds[]`, memberCount, createdAt |
| `parties/{id}/members/{uid}` | userId, role (host/guest), joinedAt |
| `parties/{id}/sync/state` | **the engine** — positionMs, isPlaying, updatedAt. Host-only write |
| `parties/{id}/messages/{id}` | userId, userName, text, createdAt (floating chat) |
| `parties/{id}/reactions/{id}` | userId, userName, emoji, createdAt (ephemeral) |
| `users/{uid}/notifications/*` | `party_invite` carries partyId + movieId/Title |

Index: `parties` — `memberIds CONTAINS` + `scheduledAt ASC` (My parties).

## The sync engine
One tiny document keeps the room aligned — there is no per-frame traffic:

- **Host** writes `{positionMs, isPlaying, updatedAt}` on play / pause / seek (hooked into the
  existing ExoPlayer listener) plus a **5s heartbeat** while playing.
- **Guests** extrapolate the host's playhead: `expected = positionMs + (now − updatedAt)` while
  playing. They mirror play/pause instantly and **hard-seek when |drift| > 2.5s**, re-checked
  every 5s so a buffering guest converges back.
- **Guest catch-up needs no special code**: a joiner's first snapshot says the room is 20 minutes
  in, the drift check sees a 20-minute gap and seeks. Same math.
- Party playback **skips the solo resume prompt** — it would fight the engine.
- Host exiting the player publishes `isPlaying: false`, pausing the room where it stands.

## Feature map
| Day | Feature |
|---|---|
| 141 | Data model + sync engine + rules + index |
| 142 | Create party (now/+30m/+1h/+2h); lobby; invite followers or a whole board; party_invite notifications; My-parties strip; `filmatube://party/{id}` |
| 143 | Synced playback room (`player/{movieId}?party={id}`); drift correction; guest catch-up |
| 144 | Floating chat + emoji reactions overlay during playback |
| 145 | Host controls; guests follow (transport read-only) |
| 146 | Leave/end; host handoff ("Make host") |
| 147 | Week review |
| 148 | **Web:** create/invite + live lobby (`/parties/[id]`) |
| 149 | **Web:** synced room (`/watch/[id]?party=…`) + floating chat/reactions |
| 150 | **Web:** host controls; guest follow |
| 151 | Parity QA + sync stress analysis |

## Parity notes (Day 151)
Engine constants are identical on both platforms — change one, change both:
`DRIFT_TOLERANCE_MS = 2500`, `HEARTBEAT_MS = 5000`, `REACTION_TTL_MS = 4000`, and the same
six reaction emoji.

Web-only differences, all deliberate:
1. **Autoplay gesture.** Browsers refuse `play()` without a user gesture. A guest has no
   transport, so a blocked autoplay would strand them on a frozen frame — `usePartySync`
   catches the rejection and shows a one-tap "Watch together" prompt. Android has no such rule.
2. **`?party=` is validated server-side.** The watch route only enables sync when `getParty`
   confirms the viewer is a member *and* the party is live for that movie — a guessed id can't
   drive or observe someone else's room. (Rules would reject the writes regardless; this stops
   the read too.)
3. **Guest transport is disabled, not hidden.** Web keeps the scrubber visible (progress is
   useful) but `seek`/`togglePlay` are no-ops; Android hides the centre transport entirely.
4. **Lobby names.** Web streams member *roles* live but resolves display names on the server
   render, so a brand-new joiner shows without a name until the next load. Acceptable for a lobby.

## Sync stress (many guests) — analysis, not yet load-tested
The engine is **O(1) writes in the number of guests**: only the host writes, at most ~1 write per
5s plus transport changes. Guests are pure readers on a single document, so 5 or 500 guests cost
the host the same. Firestore fans the snapshot out.

What *does* scale with guests is **chat + reactions**: every guest write is a document, and every
guest listens to both collections. A 50-guest room mashing emoji is the realistic hot spot — each
reaction is a doc write plus a fan-out to every listener. Mitigations if it bites: client-side
rate-limit the reaction bar, batch emoji into a counter doc, or drop the reactions listener
`limit` from 12/20.

**Not yet verified under real load** — this is reasoning from the write pattern, not a measured
test. A scripted N-client harness is the honest next step before claiming a guest ceiling.

## Rules (deployed)
- **Party read:** host **or** member only — parties are private by design.
- **create:** `hostId == auth.uid && status == 'scheduled'`.
- **update:** host/admin full; anyone else only a **membership-only** change
  (`hasOnly(['memberIds','memberCount'])`) toggling their **own** uid — same pattern as boards.
- **sync/state:** read = members; **write = host only**. This is what makes the host authoritative
  in fact, not just in UI.
- **messages:** members create with their own uid; author/host/admin delete; no edits.
- **reactions:** members create with their own uid; write-once.

## Design decisions worth remembering
- **Handoff is explicit, never automatic.** Rules cannot verify "the old host is really gone", so
  a member self-claiming `hostId` would let any guest hijack the room. The host taps **Make host**;
  a host who drops without handing off leaves the room stale until they end it.
- **Guest transport is hidden, not just ignored.** Before Day 145 a guest could seek and get yanked
  back by the drift loop ~5s later, which read as a bug. Now they see "The host controls playback".
- **Reactions are ephemeral by convention, not by TTL.** The UI shows only the last 4s and tracks
  spent ids so they don't replay on recomposition — the docs themselves accumulate (see gaps).

## Runtime checklist (needs two accounts)
**Do at least one pass cross-platform** (Android host + web guest, then swap) — that's the whole
point of sharing the schema.

- [ ] Movie detail → **Watch party** → pick a start → lobby appears with you as host.
- [ ] Invite followers / invite a board → each invitee gets a **party invite** → tap opens the lobby.
- [ ] Guest joins → appears in the guest list; member count increments.
- [ ] Host **Start the party** → both see **Watch together** → both enter the player.
- [ ] Host pauses → guest pauses within ~1s. Host seeks → guest follows.
- [ ] Guest sees **no transport** and a "host controls playback" pill; scrubber disabled.
- [ ] A guest joining **late** jumps straight to the host's position.
- [ ] Chat: type on one device, appears on the other; emoji rise and fade on both.
- [ ] Host → **Make host** on a guest → roles swap live; new host gets transport.
- [ ] Host **End the party** → everyone sees the notice and leaves the player.
- [ ] `filmatube://party/{id}` opens the lobby.

## Known gaps (tracked)
- **FCM push** for party invites still needs the Cloud Function (in-app inbox works) — same gap as
  board invites.
- **Reaction/message docs accumulate** — nothing prunes `parties/{id}/reactions`. A scheduled
  Function (or TTL policy) should sweep ended parties.
- **No automatic host-drop detection** — guests aren't told the host went silent; the room just
  stops advancing. Could surface a "host may have left" banner off the sync `updatedAt` age.
- **Ended parties are never deleted**; `observeMyParties` filters them client-side.
- Clock skew: extrapolation uses the device clock against a server `updatedAt`. A badly-skewed
  device drifts by its skew; a server-time offset probe would tighten this.

## Status
Android `assembleDebug` and web `npm run build` both green. Parties are end-to-end on **both**
platforms: create → invite → lobby → start → synced playback with chat/reactions → host controls
→ handoff/end, all off one shared schema.
