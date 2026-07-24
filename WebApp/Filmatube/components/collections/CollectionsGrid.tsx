"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { CollectionCard } from "@/components/collections/CollectionCard";
import type { Collection } from "@/lib/collections";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** The collections grid with a name filter. Client-side — the full list is already loaded. */
export function CollectionsGrid({
  collections,
  dict,
}: {
  collections: Collection[];
  dict: Dictionary["catalog"];
}) {
  const [q, setQ] = useState("");

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) return collections;
    return collections.filter((c) => c.title.toLowerCase().includes(term));
  }, [q, collections]);

  return (
    <div className="space-y-5">
      <div className="relative max-w-sm">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" aria-hidden />
        <input
          type="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={dict.searchCollections}
          className="h-10 w-full rounded-lg border border-surface-border bg-surface pl-9 pr-3 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-400"
        />
      </div>

      {filtered.length === 0 ? (
        <p className="py-16 text-center text-ink-muted">{dict.noResults}</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
          {filtered.map((col) => (
            <CollectionCard key={col.id} collection={col} isOwner />
          ))}
        </div>
      )}
    </div>
  );
}
