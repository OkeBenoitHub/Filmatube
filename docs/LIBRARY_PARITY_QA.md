# Library Parity QA — Android ↔ Web (Day 83)

Covers **Watch Later**, **My Library / My Stuff**, and **Collections**, and how they sync across
Android and Web. Both clients use the same Firestore paths in project **filmatubelive**.

## Shared data model
| Feature | Firestore path | Android | Web |
|---|---|---|---|
| Watch Later | `watchlists/{uid}/movies/{movieId}` | `WatchlistRepository` (realtime toggle) | `SaveButton` (client Firestore toggle) |
| Continue Watching | `watchProgress/{uid}/items/{movieId}` | player writes | player writes |
| Collections | `collections/{id}` (+ `/items/{movieId}`) | — (read-capable later) | full create/edit/reorder/share |

- **Owner-scoped by rules:** `watchlists/{uid}` and a collection's `userId` gate writes; public
  collections (`isPublic: true`) are readable by any signed-in user.

## Bidirectional sync (verified by design)
- [ ] **Watch Later — Android → Web:** tap the bookmark on a movie in the Android app →
      it appears under **Watch Later** on the web `/library` (and the web detail bookmark shows filled).
- [ ] **Watch Later — Web → Android:** save a movie on the web → it appears in Android **My Library**;
      both use realtime listeners, so it updates without a manual refresh.
- [ ] **Un-save** on either platform removes it on both.
- [ ] **Continue Watching** already syncs (see `CROSS_DEVICE_PLAYBACK_QA.md`); it appears in the web
      My Stuff hub and the Android Home row from the same `watchProgress` docs.

## Collections
- **Web is the authoring surface** (Days 79–81): create, edit title/cover, public/private, add/remove
  movies, reorder, share (OG cards), and clone others' public collections.
- Collections are stored in the shared `collections/{id}` + `/items/{movieId}` schema, so a future
  **Android collections UI can read/consume them** without a data migration. Until then, collections
  are a **web-only** surface (expected, not a bug).
- [ ] **Public collection link** opens for any signed-in user and renders read-only with a
      "Save to my collections" action; private collections 404 for non-owners.

## Expected differences (not bugs)
- Collections UI is web-only today (Android build is a later phase); the schema is shared.
- Cover images upload to the **avatars** R2 bucket (user-writable) because the images bucket presign
  is admin-only.
- Web adds a My Stuff hub (`/library`) and PWA save-for-later; Android surfaces Library via Settings.

## Status
Both builds green as of Week 12 close — Android `assembleDebug`, Web `next build` (31 routes).
Watch Later + Continue Watching are bidirectional today; Collections share the schema for future
Android parity. Runtime checklist is for manual verification with one account on both platforms.
