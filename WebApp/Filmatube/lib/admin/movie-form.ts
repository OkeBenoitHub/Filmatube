export interface CastEntry {
  name: string;
  character: string;
  photoUrl: string;
}

/** Flat editable movie fields used by the admin form + save action. */
export interface MovieFormValues {
  imdbId: string;
  tmdbId: string;
  titleEn: string;
  titleFr: string;
  descEn: string;
  descFr: string;
  year: number;
  duration: number;
  ageRating: string;
  genres: string[];
  directors: string[];
  cast: CastEntry[];
  posterUrl: string;
  backdropUrl: string;
  trailerUrl: string;
  videoKey: string;
  status: "draft" | "published";
  isFeatured: boolean;
  isPinned: boolean;
  isComingSoon: boolean;
}

export const emptyMovie: MovieFormValues = {
  imdbId: "",
  tmdbId: "",
  titleEn: "",
  titleFr: "",
  descEn: "",
  descFr: "",
  year: new Date().getFullYear(),
  duration: 0,
  ageRating: "",
  genres: [],
  directors: [],
  cast: [],
  posterUrl: "",
  backdropUrl: "",
  trailerUrl: "",
  videoKey: "",
  status: "draft",
  isFeatured: false,
  isPinned: false,
  isComingSoon: false,
};
