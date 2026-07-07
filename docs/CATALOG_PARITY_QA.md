# Catalog Parity QA — Android ↔ Web (Day 42)

Verifies the **movie catalog** looks and behaves the same on Android and Web.
Both clients read the **same `movies` collection** in Firebase project **filmatubelive**;
posters/backdrops/subtitles/video live in **Cloudflare R2** and are referenced by URL/key only.

## Guaranteed by design
- **One source of truth:** the Web Admin CMS (Days 36–41) writes every movie; both apps read
  the same documents. Publish/unpublish and the featured/pinned/coming-soon flags set in the CMS
  drive what each platform shows.
- **Same document shape:** `title`/`description` are `{en,fr}` maps; `genres[]` use the shared
  genre keys (`action`…`western`); `posterUrl`, `backdropUrl`, `trailerUrl`, `videoKey`,
  `subtitleTracks[{lang,url}]`, `year`, `duration`, `ageRating`, `cast[]`, `directors[]`,
  `averageRating`, `viewsCount`, `isFeatured`, `isComingSoon`, `status`.
- **Same queries:** each Web selector mirrors an Android `MovieRepository` method —

  | Row / screen | Android (`MovieRepositoryImpl`) | Web (`lib/movies.ts`) |
  |---|---|---|
  | Featured hero | `getFeatured` | `pickFeatured` |
  | Trending | `getTrending` (by `viewsCount`) | `pickTrending` |
  | New Releases | `getNewReleases` (by `addedAt`) | `pickNewReleases` |
  | Coming Soon | `getComingSoon` | `pickComingSoon` |
  | Genre rows | `getByGenre` | `pickByGenre` |
  | Detail | `getMovie` (published only) | `getMovie` |
  | More like this | `getRelated` (shared genres) | `pickRelated` |
  | Browse sort/filter | `browse` (newest/rating/A–Z, genre, year) | `/browse` + `BrowseControls` |
  | Search | `search` (title/cast/director) | `/api/movies/search` + `searchMovies` |

- **Same localization:** titles/overviews follow the app language (EN/FR) with fallback to the
  other language; Android via `strings.xml`/`Movie.title(locale)`, Web via `localized()` + the
  shared dictionary — **no hardcoded UI text** on either platform.
- **Same visibility rules:** only `status == "published"` movies are shown; drafts are hidden
  (Web `getMovie` returns null → `notFound`). Coming-soon titles appear in their own row and show
  a disabled Play with a "Coming Soon" label on the detail page.

## Expected differences (not bugs)
- **Query execution:** Android runs per-row Firestore queries (needs the composite indexes in
  `firestore.indexes.json`); Web does **one** `status == published` read and derives every row in
  memory (index-safe), so the web catalog needs no composite indexes.
- **Layout:** Android uses bottom-nav + LazyRow/LazyVerticalGrid; Web uses a top nav header
  (Home/Browse/Search) with horizontally scrolling rows and a responsive poster grid.
- **Playback:** `Play` is live on Android's player track; on Web it routes to `/watch/[id]`,
  a branded placeholder until the web player (Day 50).

## Manual checklist
Seed data with `node --env-file=.env.local scripts/seed-movies.mjs` (or create movies in the CMS),
run Web with `npm run dev` in `WebApp/Filmatube`, Android on an emulator/device.

- [ ] **Home**: same featured hero + Trending / New Releases / genre / Coming Soon rows on both.
- [ ] **Language**: switch EN↔FR → titles/overviews change on both; a FR-only field falls back to EN.
- [ ] **Browse**: genre chip + sort (newest/rating/A–Z) + year filter yield the same set on both.
- [ ] **Detail**: same backdrop/poster/meta/genres/overview/cast; trailer opens; related row matches.
- [ ] **Search**: same results for a query (title, cast member, director); trending shows when idle.
- [ ] **Draft vs published**: unpublish in the CMS → the movie disappears from both catalogs;
      publish → it reappears. Coming-soon shows the disabled Play state.

## Status
Week 6 / Phase 3 close: **Web `next build` green (27 routes); Android `assembleDebug` green.**
The runtime checklist is for manual verification once R2 buckets + seeded/CMS movies are available.
