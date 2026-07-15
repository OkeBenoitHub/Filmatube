import { describe, expect, it } from "vitest";
import { localized, searchMovies, pickFeatured, pickComingSoon, type CatalogMovie } from "@/lib/movies";

function movie(over: Partial<CatalogMovie>): CatalogMovie {
  return {
    id: "m",
    title: { en: "Title", fr: "Titre" },
    description: { en: "", fr: "" },
    posterUrl: "",
    backdropUrl: "",
    trailerUrl: null,
    genres: [],
    year: 2024,
    duration: 100,
    ageRating: "",
    cast: [],
    directors: [],
    averageRating: 0,
    ratingsCount: 0,
    likesCount: 0,
    viewsCount: 0,
    isFeatured: false,
    isComingSoon: false,
    subtitleTracks: [],
    ...over,
  } as CatalogMovie;
}

describe("localized", () => {
  it("returns the requested locale, falling back to en", () => {
    expect(localized({ en: "Hello", fr: "Bonjour" }, "fr")).toBe("Bonjour");
    expect(localized({ en: "Hello", fr: "" }, "fr")).toBe("Hello");
    expect(localized(undefined, "en")).toBe("");
  });
});

describe("searchMovies", () => {
  const list = [
    movie({ id: "a", title: { en: "The Last Harvest", fr: "La Dernière Moisson" } }),
    movie({ id: "b", title: { en: "Neon Horizon", fr: "Horizon Néon" } }),
  ];
  it("matches on the English title, case-insensitively", () => {
    expect(searchMovies(list, "harvest").map((m) => m.id)).toEqual(["a"]);
  });
  it("matches on the French title", () => {
    expect(searchMovies(list, "néon").map((m) => m.id)).toEqual(["b"]);
  });
  it("returns nothing for a blank query", () => {
    expect(searchMovies(list, "  ")).toEqual([]);
  });
});

describe("pick selectors", () => {
  const list = [
    movie({ id: "f", isFeatured: true }),
    movie({ id: "s", isComingSoon: true }),
    movie({ id: "fs", isFeatured: true, isComingSoon: true }),
  ];
  it("pickFeatured excludes coming-soon", () => {
    expect(pickFeatured(list).map((m) => m.id)).toEqual(["f"]);
  });
  it("pickComingSoon returns coming-soon only", () => {
    expect(pickComingSoon(list).map((m) => m.id).sort()).toEqual(["fs", "s"]);
  });
});
