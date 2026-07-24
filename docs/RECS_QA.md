# Recommendations — QA, relevance tuning & Week 28 review

Covers the v1.3 recommendation engine (Days 183–194) and the Day 195 QA/tuning pass.

## What the recommender is

A nightly, content-overlap recommender — **no ML**. `functions/buildRecommendations` (scheduled
every 24h, `europe-west4`, co-located with the eur3 DB) reads the published catalogue once, then
for each recently-active user builds a taste profile from the signals they already leave and
scores every unseen movie by genre/person overlap. It writes `recs/{uid}` = `{ topPicks, rows,
generatedAt }`.

The scoring lives in **`functions/recs-core.js`**, imported by both the Cloud Function and
`WebApp/Filmatube/scripts/seed-recs-demo.mjs` — so the demo/QA path exercises the *exact* ranking
that ships, not a copy.

### Signals → weight
`watched 2 · watchlist 1 · like 2 · love 3 · fire 2 · mind_blown 3 · boring 0`. Each seed's weight
is spread over its genres and its people (cast + directors), with people counted ×2 — a shared lead
or director is a stronger signal than a shared genre. `boring` and every non-watched watch-progress
entry mark a movie *seen* (excluded) without endorsing its genres. `recFeedback` dismissals ("Not
interested") are also marked seen, so a dismissal sticks across rebuilds.

### Outputs
- **topPicks** — up to 20, the flat "Top picks for you" row (overall ranking).
- **rows** — up to 4 "Because you watched X" rails, each seeded by a strongly-weighted title and
  filled with candidates that share its genre or people. A rail needs ≥3 titles or it's dropped.

The clients surface these plus two rows computed client-side (not from the rec doc): "From people
you follow" (the viewer's feed aggregated by movie) and "Hidden gems"/"New for you" (Android).

## Day 195 relevance tuning

Two changes to `recs-core.js`, both verified by the tests below and reflected in the redeployed
function + refreshed demo data:

1. **Coming-soon titles are never recommended.** Candidates are now filtered by
   `!m.isComingSoon`. Recommending a movie the viewer can't watch yet is a dead end, not a
   recommendation. (Observed live: a scifi seed's rail dropped from 11 → 10 once the unreleased
   *Echoes of Tomorrow* was excluded.)
2. **A gentle recency nudge.** Alongside the existing popularity tie-breaker, score now adds a small
   recency term (`max(0, year − (thisYear−10)) × 0.005`, ≈0.05 max) so a fresh title edges out an
   interchangeable older one. Like the popularity nudge it's far too small to outweigh a real
   genre/person overlap — it only orders otherwise-tied candidates.

Deliberately **not** changed: no negative weighting (a "boring" reaction excludes, it doesn't
penalise genres), and no genre-count normalisation — multi-genre titles legitimately match more
tastes. Revisit if topPicks start skewing toward kitchen-sink titles once the catalogue is larger.

## Automated tests

`WebApp/Filmatube/test/recs.test.ts` runs the real `buildRecsForUser` / `buildAllRecs` against an
in-memory Firestore fake (7 tests, part of `npm test`):

| Test | Asserts |
|---|---|
| no signal | returns false, writes nothing (new users get editorial rows, not noise) |
| overlap ranking | seed marked seen; genre+actor overlap outranks unrelated titles |
| coming-soon | never in topPicks or rails |
| dismissals | `recFeedback` titles excluded |
| rail threshold | every "because you watched" rail has ≥3 titles |
| topPicks cap | ≤20 even with 30 candidates |
| buildAllRecs | only active users *with signal* get a doc |

## Manual checklist (against seeded prod data)

Seed with `node --env-file=.env.local scripts/seed-recs-demo.mjs`, then on a personal demo account:
- [ ] Home shows Top picks, ≥1 "Because you watched X", From people you follow, Hidden gems/New for you.
- [ ] "Not interested" on a rec tile removes it immediately and it stays gone after reload.
- [ ] No unreleased (coming-soon) title appears in any rec rail.
- [ ] Curated/pinned rows (Day 193) sit above the rec rails; unpinned below.
- [ ] Featured editorial collections (Day 194) show in the Home marquee.

## Known limits / follow-ups
- Recs are a nightly batch — a fresh signal isn't reflected until the next run (by design).
- "From people you follow" reads the viewer's own fanned-out feed, not a global trend.
- Cold-start users (no signal) get no rec doc; they rely on editorial + catalogue rows.
- Relevance is only as good as the catalogue's genre/cast metadata — sparse cast lists weaken the
  person signal.

## Week 28 review (Days 190–196)

Recommendations (web) + admin curation, all shipped and verified:
- **190/191** — web rec rails (Because you watched / Top picks / From people you follow); detail
  "More like this" already existed.
- **192** — "Not interested" in the web movie menu (rec rows only), writing `recFeedback`.
- **193** — admin curation: manual Home-row builder + pin/boost + scheduled campaigns (`homeRows`).
- **194** — editorial collections, admin-curated and featured on Home (reusing `collections`).
- **195** — this pass: QA harness on the real scorer + coming-soon/recency tuning; function
  redeployed, demo data refreshed.
- **Milestone:** the whole v1.3 rec pipeline is now **seeded and live** on the two personal demo
  accounts — nightly build → rails → feedback loop → admin curation on top — verified end to end,
  not just compiled.

Both platforms green (Android `:app:compileDebugKotlin`; web `tsc` + `next lint` + 17 Vitest tests).
