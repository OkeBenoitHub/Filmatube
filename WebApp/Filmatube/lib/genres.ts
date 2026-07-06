/** Canonical movie genre keys (match Android + `dict.genres` labels). */
export const GENRE_KEYS = [
  "action", "adventure", "animation", "comedy", "crime", "documentary",
  "drama", "family", "fantasy", "history", "horror", "music",
  "mystery", "romance", "scifi", "thriller", "war", "western",
] as const;

export type GenreKey = (typeof GENRE_KEYS)[number];
