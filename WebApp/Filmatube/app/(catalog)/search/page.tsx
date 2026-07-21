import { Search } from "lucide-react";
import { SearchClient } from "@/components/catalog/SearchClient";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getPublishedMovies, localized, pickTrending } from "@/lib/movies";

export default async function SearchPage() {
  const [locale, dict, movies] = await Promise.all([getLocale(), getDict(), getPublishedMovies()]);
  const c = dict.catalog;

  const trending = pickTrending(movies)
    .slice(0, 12)
    .map((m) => ({
      id: m.id,
      title: localized(m.title, locale),
      posterUrl: m.posterUrl,
      year: m.year,
      isComingSoon: m.isComingSoon,
    }));

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      {/* ── Hero header (Spotitube pattern, green) ─────────────── */}
      <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-end">
        <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-900 shadow-xl shadow-brand-900/40 sm:h-28 sm:w-28">
          <Search className="h-10 w-10 text-white sm:h-12 sm:w-12" aria-hidden />
        </div>
        <div className="text-center sm:text-left">
          <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{c.searchEyebrow}</p>
          <h1 className="mt-1 text-3xl font-black leading-tight tracking-tight text-ink md:text-4xl">
            {c.search}
          </h1>
          <p className="mt-2 text-sm text-ink-muted">{c.searchSubtitle}</p>
        </div>
      </div>

      <div className="mt-12">
        <SearchClient dict={c} trending={trending} />
      </div>
    </div>
  );
}
