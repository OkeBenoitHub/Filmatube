import { NextResponse, type NextRequest } from "next/server";
import { getRequestUser } from "@/lib/auth/request-user";

const IMG = "https://image.tmdb.org/t/p";

// TMDB genre name -> our genre keys.
const GENRE_MAP: Record<string, string> = {
  Action: "action",
  Adventure: "adventure",
  Animation: "animation",
  Comedy: "comedy",
  Crime: "crime",
  Documentary: "documentary",
  Drama: "drama",
  Family: "family",
  Fantasy: "fantasy",
  History: "history",
  Horror: "horror",
  Music: "music",
  Mystery: "mystery",
  Romance: "romance",
  "Science Fiction": "scifi",
  Thriller: "thriller",
  War: "war",
  Western: "western",
};

export async function POST(request: NextRequest) {
  const user = await getRequestUser(request);
  if (!user || user.admin !== true) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const key = process.env.TMDB_API_KEY;
  if (!key) {
    return NextResponse.json({ error: "TMDB_API_KEY not configured" }, { status: 500 });
  }

  const { id } = (await request.json().catch(() => ({}))) as { id?: string };
  if (!id) return NextResponse.json({ error: "Missing id" }, { status: 400 });

  try {
    let tmdbId = id.trim();
    let imdbId = "";
    if (tmdbId.startsWith("tt")) {
      imdbId = tmdbId;
      const find = await fetchJson(
        `https://api.themoviedb.org/3/find/${tmdbId}?external_source=imdb_id&api_key=${key}`,
      );
      const found = find.movie_results?.[0];
      if (!found) return NextResponse.json({ error: "Not found" }, { status: 404 });
      tmdbId = String(found.id);
    }

    const [en, fr] = await Promise.all([
      fetchJson(
        `https://api.themoviedb.org/3/movie/${tmdbId}?append_to_response=credits,release_dates,videos&api_key=${key}&language=en-US`,
      ),
      fetchJson(`https://api.themoviedb.org/3/movie/${tmdbId}?api_key=${key}&language=fr-FR`),
    ]);

    const usRelease = en.release_dates?.results?.find((r: { iso_3166_1: string }) => r.iso_3166_1 === "US");
    const ageRating =
      usRelease?.release_dates?.map((d: { certification: string }) => d.certification).find((c: string) => c) ?? "";

    const trailer = en.videos?.results?.find(
      (v: { type: string; site: string; key: string }) => v.type === "Trailer" && v.site === "YouTube",
    );

    const genres = [
      ...new Set(
        (en.genres ?? [])
          .map((g: { name: string }) => GENRE_MAP[g.name])
          .filter(Boolean) as string[],
      ),
    ];

    return NextResponse.json({
      imdbId: imdbId || en.imdb_id || "",
      tmdbId,
      titleEn: en.title ?? "",
      titleFr: fr.title ?? "",
      descEn: en.overview ?? "",
      descFr: fr.overview ?? "",
      year: en.release_date ? Number(en.release_date.slice(0, 4)) : 0,
      duration: en.runtime ?? 0,
      ageRating,
      genres,
      posterUrl: en.poster_path ? `${IMG}/w500${en.poster_path}` : "",
      backdropUrl: en.backdrop_path ? `${IMG}/w1280${en.backdrop_path}` : "",
      trailerUrl: trailer ? `https://www.youtube.com/watch?v=${trailer.key}` : "",
      directors: (en.credits?.crew ?? [])
        .filter((c: { job: string }) => c.job === "Director")
        .map((c: { name: string }) => c.name),
      cast: (en.credits?.cast ?? []).slice(0, 12).map((c: { name: string; character: string; profile_path: string | null }) => ({
        name: c.name,
        character: c.character ?? "",
        photoUrl: c.profile_path ? `${IMG}/w185${c.profile_path}` : "",
      })),
    });
  } catch {
    return NextResponse.json({ error: "TMDB request failed" }, { status: 502 });
  }
}

async function fetchJson(url: string) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`TMDB ${res.status}`);
  return res.json();
}
