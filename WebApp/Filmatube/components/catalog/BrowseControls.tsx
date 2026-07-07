"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { GENRE_KEYS } from "@/lib/genres";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { Chip } from "@/components/account/Chip";

/** Genre / sort / year filters for the browse grid; state lives in the URL. */
export function BrowseControls({
  dict,
  genres,
  years,
}: {
  dict: Dictionary["catalog"];
  genres: Dictionary["genres"];
  years: number[];
}) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  const genre = params.get("genre") ?? "";
  const sort = params.get("sort") ?? "newest";
  const year = params.get("year") ?? "";

  const setParam = (key: string, value: string) => {
    const next = new URLSearchParams(params.toString());
    if (value) next.set(key, value);
    else next.delete(key);
    router.push(`${pathname}?${next.toString()}`, { scroll: false });
  };

  const selectClass =
    "h-10 rounded-lg border border-surface-border bg-surface px-3 text-sm text-ink";

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        <Chip label={dict.all} selected={genre === ""} onClick={() => setParam("genre", "")} />
        {GENRE_KEYS.map((key) => (
          <Chip key={key} label={genres[key]} selected={genre === key} onClick={() => setParam("genre", key)} />
        ))}
      </div>
      <div className="flex flex-wrap gap-3">
        <select value={sort} onChange={(e) => setParam("sort", e.target.value)} className={selectClass}>
          <option value="newest">{dict.sortNewest}</option>
          <option value="rating">{dict.sortRating}</option>
          <option value="az">{dict.sortAz}</option>
        </select>
        <select value={year} onChange={(e) => setParam("year", e.target.value)} className={selectClass}>
          <option value="">{dict.allYears}</option>
          {years.map((y) => (
            <option key={y} value={String(y)}>
              {y}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
