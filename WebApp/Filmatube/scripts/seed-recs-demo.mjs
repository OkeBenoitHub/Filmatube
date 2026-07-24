// Seed a demo dataset that makes the recommendation rails visibly populate, then run the REAL
// scorer (functions/recs-core.js) to write recs/{uid} — no waiting for the nightly schedule.
//
// It (1) upserts a denser catalogue with overlapping genres/cast so "Because you watched" rows
// can form, (2) writes watch/like/reaction/watchlist signals + a Continue Watching item for the
// two personal accounts, (3) writes feed events so "From people you follow" has data, then
// (4) calls buildAllRecs — the exact function the nightly job runs.
//
// Usage (Node 20+, from WebApp/Filmatube):
//   node --env-file=.env.local scripts/seed-recs-demo.mjs
//
// Idempotent: fixed doc ids, merges. Signals/feed are demo data for the two accounts below.

import { cert, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import recsCore from "../../../functions/recs-core.js";

const { buildAllRecs } = recsCore;

initializeApp({
  credential: cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n"),
  }),
});
const db = getFirestore();

const TRAILER = "https://www.youtube.com/watch?v=aqz-KE-bpKQ";
const poster = (s) => `https://picsum.photos/seed/${s}/500/750`;
const backdrop = (s) => `https://picsum.photos/seed/${s}-bd/1280/720`;
const actor = (name, character) => ({
  name,
  character,
  photoUrl: `https://picsum.photos/seed/${name.toLowerCase().replace(/[^a-z]/g, "")}/200/200`,
});

// ── The two personal accounts (human names in /users). Inc accounts are left untouched. ──
const BENOIT = "G13siOkqyFMaZNGYuq2zQqYa5c42"; // action, crime, adventure, documentary
const PRESLY = "YiJwpSycCpXO8N9a41uas8RcoJe2"; // action, adventure, horror, scifi

// ── Catalogue: dense overlap of genres and a shared cast/director pool. ──
const m = (id, title, genres, opts) => ({ id, title, genres, ...opts });
const movies = [
  m("steel-verdict", { en: "Steel Verdict", fr: "Verdict d'Acier" }, ["action", "crime", "thriller"],
    { year: 2024, views: 88000, rating: 4.5, cast: [actor("Marcus Lee", "Reyes"), actor("Sofia Blanc", "DA Cole")], directors: ["Leon Marchetti"] }),
  m("iron-alibi", { en: "Iron Alibi", fr: "Alibi de Fer" }, ["crime", "thriller", "drama"],
    { year: 2023, views: 42000, rating: 4.2, cast: [actor("Sofia Blanc", "Mara"), actor("Idris Vann", "Detective Poll")], directors: ["Clara Nguyen"] }),
  m("shadow-precinct", { en: "Shadow Precinct", fr: "Le Commissariat de l'Ombre" }, ["crime", "action"],
    { year: 2025, views: 60000, rating: 4.3, cast: [actor("Marcus Lee", "Sgt. Kade"), actor("Dax Moreau", "Internal Affairs")], directors: ["Leon Marchetti"] }),
  m("the-getaway-mile", { en: "The Getaway Mile", fr: "Le Dernier Kilomètre" }, ["action", "crime", "adventure"],
    { year: 2024, views: 51000, rating: 4.1, cast: [actor("Dax Moreau", "Wheels"), actor("Nadia Ford", "Sable")], directors: ["Leon Marchetti"] }),
  m("desert-reckoning", { en: "Desert Reckoning", fr: "L'Heure du Désert" }, ["action", "adventure"],
    { year: 2022, views: 33000, rating: 4.0, cast: [actor("Omar Haddad", "Salim"), actor("Marcus Lee", "Cross")], directors: ["Hana Okafor"] }),
  m("summit-of-ash", { en: "Summit of Ash", fr: "Le Sommet de Cendres" }, ["adventure", "action"],
    { year: 2023, views: 21000, rating: 3.9, cast: [actor("Elena Ruiz", "Vega"), actor("Kwame Osei", "Bo")], directors: ["Ava Sorensen"] }),
  m("tidewalkers", { en: "Tidewalkers", fr: "Les Marcheurs de Marée" }, ["adventure", "family"],
    { year: 2023, views: 47000, rating: 4.0, cast: [actor("Priya Nair", "Isa"), actor("Sofia Blanc", "Mother")], directors: ["Clara Nguyen"] }),
  m("starfall-protocol", { en: "Starfall Protocol", fr: "Protocole Chute d'Étoiles" }, ["scifi", "action", "adventure"],
    { year: 2025, views: 77000, rating: 4.4, cast: [actor("Marcus Lee", "Cmdr. True"), actor("Yuki Tanaka", "Ada")], directors: ["Ava Sorensen"] }),
  m("quantum-drift", { en: "Quantum Drift", fr: "Dérive Quantique" }, ["scifi", "thriller"],
    { year: 2024, views: 39000, rating: 4.2, cast: [actor("Yuki Tanaka", "Rin"), actor("Nadia Ford", "Halveg")], directors: ["Ava Sorensen"] }),
  m("the-hollow-signal", { en: "The Hollow Signal", fr: "Le Signal Creux" }, ["scifi", "horror"],
    { year: 2024, views: 28000, rating: 3.9, cast: [actor("Tomas Vex", "Ory"), actor("Elena Ruiz", "Dr. Sane")], directors: ["Hana Okafor"] }),
  m("night-of-the-marsh", { en: "Night of the Marsh", fr: "La Nuit du Marais" }, ["horror", "thriller"],
    { year: 2023, views: 25000, rating: 3.8, cast: [actor("Tomas Vex", "Cole"), actor("Greta Lind", "Ren")], directors: ["Hana Okafor"] }),
  m("the-cellar-tapes", { en: "The Cellar Tapes", fr: "Les Cassettes de la Cave" }, ["horror"],
    { year: 2022, views: 12000, rating: 3.9, cast: [actor("Greta Lind", "Nia"), actor("Dax Moreau", "Uncle Rue")], directors: ["Hana Okafor"] }),
  m("crimson-harvest", { en: "Crimson Harvest", fr: "Moisson Cramoisie" }, ["horror", "drama"],
    { year: 2023, views: 9000, rating: 3.8, cast: [actor("Idris Vann", "Farmer Vale"), actor("Greta Lind", "Lene")], directors: ["Clara Nguyen"] }),
  m("wild-continent", { en: "Wild Continent", fr: "Continent Sauvage" }, ["documentary", "adventure"],
    { year: 2024, views: 34000, rating: 4.6, cast: [actor("Idris Vann", "Narrator")], directors: ["Kwame Osei"] }),
  m("cities-of-tomorrow", { en: "Cities of Tomorrow", fr: "Les Villes de Demain" }, ["documentary"],
    { year: 2023, views: 15000, rating: 4.3, cast: [], directors: ["Ava Sorensen"] }),
  m("the-last-reef", { en: "The Last Reef", fr: "Le Dernier Récif" }, ["documentary", "family"],
    { year: 2022, views: 8000, rating: 4.1, cast: [], directors: ["Clara Nguyen"] }),
  m("paper-lanterns", { en: "Paper Lanterns", fr: "Lanternes de Papier" }, ["animation", "family", "adventure"],
    { year: 2023, views: 56000, rating: 4.2, cast: [actor("Sofia Blanc", "Yuna (voice)"), actor("Priya Nair", "Kit (voice)")], directors: ["Pixel Studio"] }),
  m("clockwork-kingdom", { en: "Clockwork Kingdom", fr: "Le Royaume Mécanique" }, ["animation", "adventure"],
    { year: 2024, views: 44000, rating: 4.0, cast: [actor("Marcus Lee", "Gearman (voice)")], directors: ["Pixel Studio"] }),
  m("the-quiet-ledger", { en: "The Quiet Ledger", fr: "Le Registre Silencieux" }, ["drama", "crime"],
    { year: 2023, views: 19000, rating: 4.1, cast: [actor("Nadia Ford", "Auditor Wynn"), actor("Idris Vann", "Halloran")], directors: ["Clara Nguyen"] }),
  m("borrowed-light", { en: "Borrowed Light", fr: "Lumière Empruntée" }, ["drama", "romance"],
    { year: 2022, views: 22000, rating: 3.9, cast: [actor("Elena Ruiz", "Coeli"), actor("Omar Haddad", "Sami")], directors: ["Hana Okafor"] }),
  // Hidden gems: high rating, very low views (<=500) so they qualify for the "Hidden gems" rail.
  m("the-tin-forest", { en: "The Tin Forest", fr: "La Forêt de Fer-Blanc" }, ["adventure", "family"],
    { year: 2021, views: 320, rating: 4.4, cast: [actor("Priya Nair", "Ada")], directors: ["Clara Nguyen"] }),
  m("salt-and-cedar", { en: "Salt and Cedar", fr: "Sel et Cèdre" }, ["drama"],
    { year: 2020, views: 150, rating: 4.2, cast: [actor("Elena Ruiz", "Noor")], directors: ["Hana Okafor"] }),
  m("undertow-blues", { en: "Undertow Blues", fr: "Blues du Ressac" }, ["crime", "drama"],
    { year: 2021, views: 90, rating: 4.0, cast: [actor("Sofia Blanc", "Del")], directors: ["Leon Marchetti"] }),
  m("atlas-of-small-things", { en: "Atlas of Small Things", fr: "Atlas des Petites Choses" }, ["documentary"],
    { year: 2022, views: 40, rating: 4.5, cast: [], directors: ["Kwame Osei"] }),
];

for (const movie of movies) {
  await db.collection("movies").doc(movie.id).set(
    {
      type: "movie",
      title: movie.title,
      description: {
        en: `${movie.title.en} — a demo catalogue title seeded for recommendation testing.`,
        fr: `${movie.title.fr} — un titre de démonstration ajouté pour tester les recommandations.`,
      },
      genres: movie.genres,
      year: movie.year,
      duration: 100 + (movie.year % 40),
      ageRating: "PG-13",
      cast: movie.cast ?? [],
      directors: movie.directors ?? [],
      averageRating: movie.rating,
      ratingsCount: Math.round(movie.views / 40),
      viewsCount: movie.views,
      likesCount: Math.round(movie.views / 50),
      isFeatured: ["steel-verdict", "starfall-protocol"].includes(movie.id),
      isComingSoon: false,
      posterUrl: poster(movie.id),
      backdropUrl: backdrop(movie.id),
      thumbnailUrl: poster(movie.id),
      trailerUrl: TRAILER,
      videoKey: "",
      subtitleTracks: [],
      audioTracks: [],
      language: "en",
      status: "published",
      isPinned: false,
      addedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
}
console.log(`✅ upserted ${movies.length} catalogue movies`);

// ── Per-account signal profiles. Watched titles become "Because you watched X" seeds. ──
const profiles = {
  [BENOIT]: {
    watched: ["steel-verdict", "shadow-precinct", "wild-continent"],
    likes: ["the-getaway-mile", "starfall-protocol"],
    reactions: { "desert-reckoning": "love" },
    watchlist: ["cities-of-tomorrow"],
    inProgress: { id: "midnight-circuit", progress: 0.35 },
    // Feed fanned out from the other personal account — the "From people you follow" source.
    feedFrom: { uid: PRESLY, name: "presly benoit" },
    feed: ["iron-alibi", "iron-alibi", "the-quiet-ledger", "the-quiet-ledger", "summit-of-ash", "tidewalkers", "paper-lanterns"],
  },
  [PRESLY]: {
    watched: ["starfall-protocol", "night-of-the-marsh", "the-hollow-signal"],
    likes: ["quantum-drift", "summit-of-ash"],
    reactions: { "the-cellar-tapes": "fire" },
    watchlist: ["desert-reckoning"],
    inProgress: { id: "neon-horizon", progress: 0.5 },
    feedFrom: { uid: BENOIT, name: "BENOIT PRESLY NDONG OKE" },
    feed: ["clockwork-kingdom", "clockwork-kingdom", "crimson-harvest", "crimson-harvest", "borrowed-light", "tidewalkers", "the-tin-forest"],
  },
};

const titleOf = (id) => movies.find((x) => x.id === id)?.title.en ?? id;

for (const [uid, p] of Object.entries(profiles)) {
  // Mark this account active so buildAllRecs includes it.
  await db.collection("users").doc(uid).set({ lastActiveAt: FieldValue.serverTimestamp() }, { merge: true });

  for (const id of p.watched) {
    await db.collection("watchProgress").doc(uid).collection("items").doc(id).set(
      { movieId: id, isWatched: true, completed: true, progress: 1, positionMs: 0, durationMs: 0, updatedAt: FieldValue.serverTimestamp() },
      { merge: true },
    );
  }
  if (p.inProgress) {
    await db.collection("watchProgress").doc(uid).collection("items").doc(p.inProgress.id).set(
      { movieId: p.inProgress.id, isWatched: false, completed: false, progress: p.inProgress.progress, positionMs: 0, durationMs: 0, updatedAt: FieldValue.serverTimestamp() },
      { merge: true },
    );
  }
  for (const id of p.likes) {
    await db.collection("likes").doc(uid).collection("items").doc(id).set(
      { movieId: id, createdAt: FieldValue.serverTimestamp() }, { merge: true },
    );
  }
  for (const [id, reaction] of Object.entries(p.reactions)) {
    await db.collection("reactions").doc(uid).collection("items").doc(id).set(
      { movieId: id, reaction, createdAt: FieldValue.serverTimestamp() }, { merge: true },
    );
  }
  for (const id of p.watchlist) {
    await db.collection("watchlists").doc(uid).collection("movies").doc(id).set(
      { movieId: id, addedAt: FieldValue.serverTimestamp() }, { merge: true },
    );
  }
  // Feed events (fixed ids so re-runs don't pile up), each referencing a movie.
  let n = 0;
  for (const id of p.feed) {
    n += 1;
    await db.collection("feed").doc(uid).collection("events").doc(`demo-${n}`).set(
      {
        actorId: p.feedFrom.uid,
        actorName: p.feedFrom.name,
        actorAvatar: "",
        type: "watched",
        movieId: id,
        movieTitle: titleOf(id),
        createdAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
  }
  console.log(`✅ signals + feed seeded for ${uid}`);
}

// ── Run the real scorer. ──
const summary = await buildAllRecs(db, FieldValue);
console.log(`\n🎯 recs built: ${summary.built}/${summary.total} users, catalogue ${summary.catalogue}`);

for (const uid of [BENOIT, PRESLY]) {
  const doc = await db.collection("recs").doc(uid).get();
  if (!doc.exists) {
    console.log(`   ${uid}: no rec doc`);
    continue;
  }
  const d = doc.data();
  console.log(`   ${uid}: topPicks=${(d.topPicks ?? []).length}, rows=${(d.rows ?? []).length}`);
  (d.rows ?? []).forEach((r) => console.log(`      • "${r.seedTitle}" → ${r.movieIds.length} movies`));
}

console.log("\nDone. Open Home on those accounts to see the rec rails.");
process.exit(0);
