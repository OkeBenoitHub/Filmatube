"use strict";

/**
 * Shared recommendation core (v1.3, Day 183).
 *
 * Extracted from index.js so the nightly `buildRecommendations` function AND the local
 * `scripts/seed-recs-demo.mjs` run the *same* ranking — seeding demo data and watching it
 * populate exercises the real scorer, not a copy that could drift from it.
 *
 * Deliberately dependency-free: `db` and `FieldValue` are passed in, so the module works
 * identically inside a Cloud Function and in a one-off admin script.
 */

/** Only build recs for accounts active within this window — no point scoring dormant users. */
const REC_ACTIVE_DAYS = 30;
/** How many "Because you watched X" rails, and how many movies each holds. */
const REC_ROWS = 4;
const REC_PER_ROW = 12;
/** Size of the flat "For you" row. */
const REC_TOP_PICKS = 20;

/**
 * Signal → weight. What a user does with a movie is read as a positive vote of this strength,
 * spread across that movie's genres and people. "boring" isn't negative weight — it just
 * marks the movie as seen (excluded) without endorsing its genres.
 */
const REC_SIGNAL_WEIGHT = {
  watched: 2,
  watchlist: 1,
  like: 2,
  love: 3,
  fire: 2,
  mind_blown: 3,
  boring: 0,
};

/** People count for more than genre — a shared lead or director is a stronger signal. */
const REC_PERSON_MULTIPLIER = 2;

/** People a movie associates with — cast names plus directors, deduplicated. */
function moviePeople(movie) {
  const cast = (movie.cast ?? []).map((c) => c.name).filter(Boolean);
  return [...new Set([...cast, ...(movie.directors ?? [])])];
}

/**
 * Build and write one user's recs. Returns false (writes nothing) when there isn't enough
 * signal to beat guessing — a brand-new user gets the catalogue's editorial rows, not noise.
 */
async function buildRecsForUser(db, FieldValue, uid, movies) {
  const byId = new Map(movies.map((m) => [m.id, m]));

  // ── Gather signals, each keyed under the user so it's one cheap read apiece. ──
  const [watch, watchlist, likes, reactions, feedback] = await Promise.all([
    db.collection("watchProgress").doc(uid).collection("items").get(),
    db.collection("watchlists").doc(uid).collection("movies").get(),
    db.collection("likes").doc(uid).collection("items").get(),
    db.collection("reactions").doc(uid).collection("items").get(),
    db.collection("recFeedback").doc(uid).collection("items").get(),
  ]);

  // movieId → accumulated positive weight; and the set of movies to never recommend.
  const seedWeight = new Map();
  const seen = new Set();
  const bump = (movieId, w) => {
    seen.add(movieId);
    if (w > 0) seedWeight.set(movieId, (seedWeight.get(movieId) ?? 0) + w);
  };

  watch.forEach((d) => { if (d.get("isWatched")) bump(d.id, REC_SIGNAL_WEIGHT.watched); else seen.add(d.id); });
  watchlist.forEach((d) => bump(d.id, REC_SIGNAL_WEIGHT.watchlist));
  likes.forEach((d) => bump(d.id, REC_SIGNAL_WEIGHT.like));
  reactions.forEach((d) => bump(d.id, REC_SIGNAL_WEIGHT[d.get("reaction")] ?? 0));
  feedback.forEach((d) => seen.add(d.id)); // dismissed → never surface again

  if (seedWeight.size === 0) return false;

  // ── Spread each seed's weight over its genres and people. ──
  const genreW = new Map();
  const personW = new Map();
  const add = (map, key, w) => map.set(key, (map.get(key) ?? 0) + w);
  for (const [movieId, w] of seedWeight) {
    const movie = byId.get(movieId);
    if (!movie) continue;
    (movie.genres ?? []).forEach((g) => add(genreW, g, w));
    moviePeople(movie).forEach((p) => add(personW, p, w));
  }

  const nowYear = new Date().getFullYear();
  const score = (movie) => {
    let s = 0;
    (movie.genres ?? []).forEach((g) => { s += genreW.get(g) ?? 0; });
    moviePeople(movie).forEach((p) => { s += (personW.get(p) ?? 0) * REC_PERSON_MULTIPLIER; });
    // Two gentle nudges that only break ties between equally-matched candidates: popularity,
    // and recency so a fresh title edges out an interchangeable older one. Both are small
    // enough (≈0.05 max) that they never outweigh a real genre/person overlap match.
    const popularity = Math.log10((movie.viewsCount ?? 0) + 1) * 0.01;
    const recency = Math.max(0, ((movie.year ?? 0) - (nowYear - 10))) * 0.005;
    return s + popularity + recency;
  };

  // ── Score every unseen candidate once. Coming-soon titles are excluded: recommending a movie
  //    the viewer can't actually watch yet is a dead end, not a recommendation. ──
  const candidates = movies
    .filter((m) => !seen.has(m.id) && !m.isComingSoon)
    .map((m) => ({ id: m.id, movie: m, score: score(m) }))
    .filter((c) => c.score > 0)
    .sort((a, b) => b.score - a.score);

  if (candidates.length === 0) return false;

  const topPicks = candidates.slice(0, REC_TOP_PICKS).map((c) => c.id);

  // ── "Because you watched X": strongest seeds, each with candidates that actually overlap it. ──
  const topSeeds = [...seedWeight.entries()]
    .sort((a, b) => b[1] - a[1])
    .map(([id]) => byId.get(id))
    .filter(Boolean);

  const rows = [];
  const usedInRows = new Set();
  for (const seed of topSeeds) {
    if (rows.length >= REC_ROWS) break;
    const seedGenres = new Set(seed.genres ?? []);
    const seedPeople = new Set(moviePeople(seed));

    const rowMovies = candidates
      .filter((c) => !usedInRows.has(c.id))
      .filter((c) =>
        (c.movie.genres ?? []).some((g) => seedGenres.has(g)) ||
        moviePeople(c.movie).some((p) => seedPeople.has(p)),
      )
      .slice(0, REC_PER_ROW);

    // A row of one or two isn't worth a whole rail.
    if (rowMovies.length < 3) continue;
    rowMovies.forEach((c) => usedInRows.add(c.id));
    rows.push({
      seedMovieId: seed.id,
      seedTitle: (seed.title && (seed.title.en ?? "")) || "",
      seedPoster: seed.posterUrl ?? "",
      movieIds: rowMovies.map((c) => c.id),
    });
  }

  await db.collection("recs").doc(uid).set({
    topPicks,
    rows,
    generatedAt: FieldValue.serverTimestamp(),
  });
  return true;
}

/**
 * Read the published catalogue once, then build recs for every recently-active user.
 * Returns a small summary for logging. Shared by the scheduled function and the demo script.
 */
async function buildAllRecs(db, FieldValue) {
  const moviesSnap = await db.collection("movies").where("status", "==", "published").get();
  const movies = moviesSnap.docs.map((d) => ({ id: d.id, ...d.data() }));
  if (movies.length === 0) return { built: 0, total: 0, catalogue: 0 };

  const cutoff = new Date(Date.now() - REC_ACTIVE_DAYS * 24 * 3600e3);
  const users = await db.collection("users").where("lastActiveAt", ">=", cutoff).limit(500).get();

  let built = 0;
  for (const user of users.docs) {
    try {
      if (await buildRecsForUser(db, FieldValue, user.id, movies)) built += 1;
    } catch (e) {
      console.error(`recs: failed for ${user.id}`, e);
    }
  }
  return { built, total: users.size, catalogue: movies.length };
}

module.exports = {
  REC_ACTIVE_DAYS,
  REC_SIGNAL_WEIGHT,
  REC_PERSON_MULTIPLIER,
  moviePeople,
  buildRecsForUser,
  buildAllRecs,
};
