"use client";

import { useState, type MouseEvent } from "react";
import Link from "next/link";
import { Globe, MoreVertical } from "lucide-react";
import { CollectionCover } from "@/components/collections/CollectionCover";
import { CollectionMenu } from "@/components/collections/CollectionMenu";
import { useI18n } from "@/components/providers/LocaleProvider";
import type { Collection } from "@/lib/collections";
import { cn } from "@/lib/utils";

/**
 * A collection thumbnail linking to its page, with a 3-dot / right-click options menu — the
 * collections counterpart to MovieCard. The button is a sibling of the link (never nested in
 * the anchor), and right-clicking the cover opens the same menu.
 */
export function CollectionCard({
  collection,
  isOwner,
  className,
}: {
  collection: Collection;
  isOwner: boolean;
  className?: string;
}) {
  const { dict } = useI18n();
  const [menuAt, setMenuAt] = useState<{ top: number; left: number } | null>(null);

  const openAt = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setMenuAt({ top: e.clientY, left: e.clientX });
  };

  return (
    <>
      <div className={cn("group", className)}>
        <div className="relative aspect-video overflow-hidden rounded-xl border border-surface-border bg-surface-hover transition-colors group-hover:border-brand-700/60">
          <Link
            href={`/collections/${collection.id}`}
            onContextMenu={openAt}
            className="block h-full w-full"
            aria-label={collection.title || dict.catalog.collectionUntitled}
          >
            <CollectionCover coverUrl={collection.coverUrl} title={collection.title} />
          </Link>

          {collection.isPublic && (
            <span className="pointer-events-none absolute left-1.5 top-1.5 rounded bg-black/60 p-1 text-white">
              <Globe className="h-3 w-3" aria-hidden />
            </span>
          )}

          <button
            type="button"
            aria-label={dict.catalog.collectionOptions}
            onClick={openAt}
            className="absolute right-1.5 top-1.5 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-black/55 text-white opacity-0 transition-opacity hover:bg-black/75 focus-visible:opacity-100 group-hover:opacity-100 max-[768px]:opacity-100"
          >
            <MoreVertical className="h-4 w-4" aria-hidden />
          </button>
        </div>

        <Link href={`/collections/${collection.id}`} onContextMenu={openAt} className="mt-1.5 block">
          <p className="truncate text-sm text-ink">{collection.title || dict.catalog.collectionUntitled}</p>
        </Link>
      </div>

      {menuAt && (
        <CollectionMenu
          collection={collection}
          position={menuAt}
          dict={dict.catalog}
          isOwner={isOwner}
          onClose={() => setMenuAt(null)}
        />
      )}
    </>
  );
}
