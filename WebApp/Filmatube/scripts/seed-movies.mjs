// Seed a few published sample movies so the catalog UI (Android + web) shows data
// before the Admin CMS exists. Images use picsum.photos (deterministic seeds).
//
// Usage (Node 20+, loads .env.local for the service account):
//   node --env-file=.env.local scripts/seed-movies.mjs
//
// Re-running is safe (uses fixed doc ids, merges). Delete via the Firebase console.

import { cert, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";

initializeApp({
  credential: cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n"),
  }),
});
const db = getFirestore();

const TRAILER = "https://www.youtube.com/watch?v=aqz-KE-bpKQ"; // Big Buck Bunny (CC)

function poster(slug) {
  return `https://picsum.photos/seed/${slug}/500/750`;
}
function backdrop(slug) {
  return `https://picsum.photos/seed/${slug}-bd/1280/720`;
}
function actor(name, character) {
  const slug = name.toLowerCase().replace(/[^a-z]/g, "");
  return { name, character, photoUrl: `https://picsum.photos/seed/${slug}/200/200` };
}

const movies = [
  {
    id: "neon-horizon",
    title: { en: "Neon Horizon", fr: "Horizon Néon" },
    description: {
      en: "In a rain-soaked megacity, a courier uncovers a conspiracy that could rewrite reality.",
      fr: "Dans une mégalopole noyée sous la pluie, un coursier découvre un complot qui pourrait réécrire la réalité.",
    },
    genres: ["scifi", "thriller"],
    year: 2024,
    duration: 128,
    ageRating: "PG-13",
    averageRating: 4.4,
    ratingsCount: 1820,
    viewsCount: 98000,
    isFeatured: true,
    isComingSoon: false,
    directors: ["Ava Sorensen"],
    cast: [actor("Marcus Lee", "Kade"), actor("Nadia Ford", " Senna"), actor("Idris Vann", "The Broker")],
  },
  {
    id: "midnight-circuit",
    title: { en: "Midnight Circuit", fr: "Circuit de Minuit" },
    description: {
      en: "A retired driver is pulled into one last heist across a city that never sleeps.",
      fr: "Un pilote à la retraite est entraîné dans un dernier casse à travers une ville qui ne dort jamais.",
    },
    genres: ["action", "crime"],
    year: 2025,
    duration: 116,
    ageRating: "R",
    averageRating: 4.6,
    ratingsCount: 2450,
    viewsCount: 142000,
    isFeatured: true,
    isComingSoon: false,
    directors: ["Leon Marchetti"],
    cast: [actor("Marcus Lee", "Rey"), actor("Sofia Blanc", "Detective Cruz")],
  },
  {
    id: "the-last-harvest",
    title: { en: "The Last Harvest", fr: "La Dernière Moisson" },
    description: {
      en: "A family fights to keep their land through one unforgiving season.",
      fr: "Une famille se bat pour garder sa terre au fil d'une saison impitoyable.",
    },
    genres: ["drama", "history"],
    year: 2023,
    duration: 134,
    ageRating: "PG-13",
    averageRating: 4.1,
    ratingsCount: 980,
    viewsCount: 54000,
    isFeatured: false,
    isComingSoon: false,
    directors: ["Clara Nguyen"],
    cast: [actor("Nadia Ford", "Mara"), actor("Idris Vann", "Elias")],
  },
  {
    id: "whisker-tales",
    title: { en: "Whisker Tales", fr: "Contes de Moustaches" },
    description: {
      en: "A clumsy kitten leads a rooftop crew on a citywide adventure.",
      fr: "Un chaton maladroit entraîne une bande des toits dans une aventure à travers la ville.",
    },
    genres: ["animation", "family", "comedy"],
    year: 2022,
    duration: 94,
    ageRating: "G",
    averageRating: 4.0,
    ratingsCount: 1330,
    viewsCount: 76000,
    isFeatured: false,
    isComingSoon: false,
    directors: ["Pixel Studio"],
    cast: [actor("Sofia Blanc", "Mochi (voice)"), actor("Marcus Lee", "Biscuit (voice)")],
  },
  {
    id: "crimson-vow",
    title: { en: "Crimson Vow", fr: "Serment Cramoisi" },
    description: {
      en: "Two rivals discover that the line between hate and love is thinner than they thought.",
      fr: "Deux rivaux découvrent que la frontière entre la haine et l'amour est plus mince qu'ils ne le pensaient.",
    },
    genres: ["romance", "drama"],
    year: 2023,
    duration: 108,
    ageRating: "PG-13",
    averageRating: 3.8,
    ratingsCount: 720,
    viewsCount: 41000,
    isFeatured: false,
    isComingSoon: false,
    directors: ["Hana Okafor"],
    cast: [actor("Nadia Ford", "Lila"), actor("Idris Vann", "Theo")],
  },
  {
    id: "echoes-of-tomorrow",
    title: { en: "Echoes of Tomorrow", fr: "Échos de Demain" },
    description: {
      en: "A lone scientist hears signals from a future that hasn't happened yet.",
      fr: "Une scientifique solitaire capte des signaux d'un futur qui n'a pas encore eu lieu.",
    },
    genres: ["scifi", "adventure"],
    year: 2026,
    duration: 121,
    ageRating: "PG-13",
    averageRating: 0,
    ratingsCount: 0,
    viewsCount: 0,
    isFeatured: false,
    isComingSoon: true,
    directors: ["Ava Sorensen"],
    cast: [actor("Sofia Blanc", "Dr. Wren"), actor("Marcus Lee", "Cole")],
  },
];

for (const movie of movies) {
  const { id, ...rest } = movie;
  await db.collection("movies").doc(id).set(
    {
      type: "movie",
      imdbId: "",
      tmdbId: "",
      posterUrl: poster(id),
      backdropUrl: backdrop(id),
      thumbnailUrl: poster(id),
      trailerUrl: TRAILER,
      videoKey: "",
      subtitleTracks: [],
      audioTracks: [],
      language: "en",
      likesCount: 0,
      status: "published",
      isPinned: false,
      addedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
      ...rest,
    },
    { merge: true },
  );
  console.log(`✅ seeded ${id}`);
}

console.log(`\nDone — ${movies.length} movies. Restart the app to see them on Home/Browse/Search.`);
process.exit(0);
