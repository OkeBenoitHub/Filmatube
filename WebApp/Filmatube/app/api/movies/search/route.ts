import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth/session";
import { getLocale } from "@/lib/i18n/server";
import { getPublishedMovies, localized, searchMovies } from "@/lib/movies";

/** Signed-in movie search. Returns already-localized, serializable result rows. */
export async function GET(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ results: [] }, { status: 401 });

  const q = req.nextUrl.searchParams.get("q") ?? "";
  const genre = req.nextUrl.searchParams.get("genre") ?? "";
  if (!q.trim()) return NextResponse.json({ results: [] });

  const locale = await getLocale();
  const all = await getPublishedMovies();
  let results = searchMovies(all, q);
  if (genre) results = results.filter((m) => m.genres.includes(genre));

  return NextResponse.json({
    results: results.map((m) => ({
      id: m.id,
      title: localized(m.title, locale),
      posterUrl: m.posterUrl,
      year: m.year,
      isComingSoon: m.isComingSoon,
    })),
  });
}
