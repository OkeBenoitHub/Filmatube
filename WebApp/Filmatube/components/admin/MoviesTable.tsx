"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Plus } from "lucide-react";
import type { AdminMovieRow } from "@/lib/admin/movies";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/Input";

type SortKey = "title" | "year" | "status";

export function MoviesTable({
  movies,
  dict,
  heading,
}: {
  movies: AdminMovieRow[];
  dict: Dictionary["adminMovies"];
  heading: string;
}) {
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<SortKey>("year");

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    const filtered = q ? movies.filter((m) => m.title.toLowerCase().includes(q)) : movies;
    return [...filtered].sort((a, b) => {
      if (sort === "title") return a.title.localeCompare(b.title);
      if (sort === "status") return a.status.localeCompare(b.status);
      return b.year - a.year;
    });
  }, [movies, query, sort]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-ink">{heading}</h1>
        <Link
          href="/admin/movies/new"
          className="inline-flex h-9 items-center gap-2 rounded-lg bg-brand-500 px-4 text-sm font-semibold text-white hover:bg-brand-600"
        >
          <Plus className="h-4 w-4" aria-hidden />
          {dict.add}
        </Link>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Input
          placeholder={dict.search}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="max-w-xs"
        />
        <select
          value={sort}
          onChange={(e) => setSort(e.target.value as SortKey)}
          className="h-10 rounded-lg border border-surface-border bg-surface px-3 text-sm text-ink"
        >
          <option value="year">{dict.year}</option>
          <option value="title">{dict.title}</option>
          <option value="status">{dict.status}</option>
        </select>
      </div>

      {rows.length === 0 ? (
        <div className="rounded-xl border border-dashed border-surface-border p-12 text-center text-ink-muted">
          {dict.empty}
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-surface-border">
          <table className="w-full text-sm">
            <thead className="border-b border-surface-border text-left text-ink-muted">
              <tr>
                <th className="p-3 font-medium">{dict.title}</th>
                <th className="p-3 font-medium">{dict.year}</th>
                <th className="p-3 font-medium">{dict.status}</th>
                <th className="p-3" />
              </tr>
            </thead>
            <tbody>
              {rows.map((m) => (
                <tr key={m.id} className="border-b border-surface-border/60 last:border-0">
                  <td className="p-3">
                    <div className="flex items-center gap-3">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={m.posterUrl || undefined}
                        alt=""
                        className="h-12 w-8 rounded object-cover bg-surface-hover"
                      />
                      <div className="flex flex-wrap items-center gap-1.5">
                        <span className="font-medium text-ink">{m.title || "—"}</span>
                        {m.isFeatured && <Badge label={dict.featured} tone="gold" />}
                        {m.isPinned && <Badge label={dict.pinned} tone="brand" />}
                        {m.isComingSoon && <Badge label={dict.comingSoon} tone="muted" />}
                      </div>
                    </div>
                  </td>
                  <td className="p-3 text-ink-muted">{m.year || "—"}</td>
                  <td className="p-3">
                    <StatusBadge status={m.status} draftLabel={dict.draft} publishedLabel={dict.published} />
                  </td>
                  <td className="p-3 text-right">
                    <Link href={`/admin/movies/${m.id}`} className="text-brand-400 hover:underline">
                      {dict.edit}
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Badge({ label, tone }: { label: string; tone: "gold" | "brand" | "muted" }) {
  const tones = {
    gold: "bg-gold/20 text-gold",
    brand: "bg-brand-700/25 text-brand-300",
    muted: "bg-surface-hover text-ink-muted",
  };
  return <span className={cn("rounded px-1.5 py-0.5 text-[10px] font-medium", tones[tone])}>{label}</span>;
}

function StatusBadge({ status, draftLabel, publishedLabel }: { status: string; draftLabel: string; publishedLabel: string }) {
  const published = status === "published";
  return (
    <span
      className={cn(
        "rounded-full px-2 py-0.5 text-xs font-medium",
        published ? "bg-brand-500/20 text-brand-300" : "bg-surface-hover text-ink-muted",
      )}
    >
      {published ? publishedLabel : draftLabel}
    </span>
  );
}
