"use client";

import { useState, useTransition } from "react";
import Link from "next/link";
import { ArrowDown, ArrowUp, ExternalLink, Plus, Star, Trash2 } from "lucide-react";
import { Card } from "@/components/ui/Card";
import type { EditorialRow } from "@/lib/admin/editorial";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import {
  createEditorialCollection,
  deleteEditorialCollection,
  reorderFeatured,
  setCollectionFeatured,
  setCollectionSubtitle,
} from "@/app/admin/collections/actions";

type Dict = Dictionary["adminEditorial"];

export function EditorialManager({ rows, dict }: { rows: EditorialRow[]; dict: Dict }) {
  const [pending, start] = useTransition();
  const featured = rows.filter((r) => r.featured);

  const move = (i: number, dir: -1 | 1) => {
    const j = i + dir;
    if (j < 0 || j >= featured.length) return;
    const ids = featured.map((r) => r.id);
    [ids[i], ids[j]] = [ids[j], ids[i]];
    start(() => reorderFeatured(ids));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-ink">{dict.title}</h1>
          <p className="mt-1 text-sm text-ink-muted">{dict.subtitle}</p>
        </div>
        <button
          type="button"
          disabled={pending}
          onClick={() => start(() => { void createEditorialCollection(); })}
          className="inline-flex h-9 shrink-0 items-center gap-2 rounded-lg bg-brand-600 px-3 text-sm font-semibold text-white hover:bg-brand-500 disabled:opacity-50"
        >
          <Plus className="h-4 w-4" aria-hidden />
          {dict.newCollection}
        </button>
      </div>

      {rows.length === 0 ? (
        <Card className="p-8 text-center text-ink-muted">{dict.empty}</Card>
      ) : (
        <div className="space-y-2">
          {rows.map((row) => {
            const featIndex = featured.findIndex((r) => r.id === row.id);
            return (
              <RowCard
                key={row.id}
                row={row}
                dict={dict}
                pending={pending}
                featIndex={featIndex}
                featuredCount={featured.length}
                onMove={move}
                start={start}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}

function RowCard({
  row,
  dict,
  pending,
  featIndex,
  featuredCount,
  onMove,
  start,
}: {
  row: EditorialRow;
  dict: Dict;
  pending: boolean;
  featIndex: number;
  featuredCount: number;
  onMove: (i: number, dir: -1 | 1) => void;
  start: (fn: () => void) => void;
}) {
  const [subtitle, setSubtitle] = useState(row.subtitle);
  const dirty = subtitle.trim() !== row.subtitle;

  return (
    <Card className="flex flex-wrap items-center gap-3 p-3">
      {row.featured && (
        <div className="flex flex-col">
          <button type="button" disabled={pending || featIndex === 0} onClick={() => onMove(featIndex, -1)} className="text-ink-faint hover:text-ink disabled:opacity-30" aria-label={dict.moveUp}>
            <ArrowUp className="h-4 w-4" />
          </button>
          <button type="button" disabled={pending || featIndex === featuredCount - 1} onClick={() => onMove(featIndex, 1)} className="text-ink-faint hover:text-ink disabled:opacity-30" aria-label={dict.moveDown}>
            <ArrowDown className="h-4 w-4" />
          </button>
        </div>
      )}

      <div className="h-12 w-20 shrink-0 overflow-hidden rounded border border-surface-border bg-surface-hover">
        {row.coverUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={row.coverUrl} alt="" className="h-full w-full object-cover" />
        )}
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate font-semibold text-ink">{row.title || dict.untitled}</p>
        <p className="text-xs text-ink-muted">{dict.movieCount.replace("{n}", String(row.movieCount))}</p>
      </div>

      {/* Editorial subtitle — the tagline under the title on the Home card. */}
      <div className="flex items-center gap-1.5">
        <input
          value={subtitle}
          onChange={(e) => setSubtitle(e.target.value)}
          placeholder={dict.subtitlePlaceholder}
          className="h-8 w-48 rounded-lg border border-surface-border bg-surface px-2.5 text-[13px] text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-400"
        />
        {dirty && (
          <button type="button" disabled={pending} onClick={() => start(() => setCollectionSubtitle(row.id, subtitle))} className="h-8 rounded-lg bg-brand-600 px-2.5 text-xs font-semibold text-white hover:bg-brand-500 disabled:opacity-50">
            {dict.saveSubtitle}
          </button>
        )}
      </div>

      <button
        type="button"
        disabled={pending}
        onClick={() => start(() => setCollectionFeatured(row.id, !row.featured))}
        className={`inline-flex h-8 items-center gap-1.5 rounded-lg border px-3 text-[13px] font-medium transition-colors disabled:opacity-50 ${
          row.featured ? "border-amber-500/50 bg-amber-500/15 text-amber-300" : "border-surface-border text-ink-muted hover:bg-surface-hover"
        }`}
      >
        <Star className={`h-3.5 w-3.5 ${row.featured ? "fill-amber-300" : ""}`} aria-hidden />
        {row.featured ? dict.featured : dict.feature}
      </button>

      <Link href={`/collections/${row.id}`} className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-surface-border px-3 text-[13px] font-medium text-ink hover:bg-surface-hover">
        <ExternalLink className="h-3.5 w-3.5" aria-hidden />
        {dict.editContent}
      </Link>

      <button
        type="button"
        disabled={pending}
        onClick={() => { if (confirm(dict.confirmDelete)) start(() => deleteEditorialCollection(row.id)); }}
        className="rounded-lg border border-surface-border p-2 text-red-400 hover:bg-red-500/10 disabled:opacity-50"
        aria-label={dict.delete}
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </Card>
  );
}
