"use client";

import { useState, type MouseEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { MoreVertical } from "lucide-react";
import { deleteDoc, doc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { MovieMenu } from "@/components/catalog/MovieMenu";
import { useAuth } from "@/components/providers/AuthProvider";
import { useI18n } from "@/components/providers/LocaleProvider";
import { localized, type ContinueWatchingItem } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";
import { cn } from "@/lib/utils";

/**
 * A Continue Watching tile — poster + resume progress bar, linking to the player. Like MovieCard
 * it carries a 3-dot / right-click options menu, plus a "Remove from Continue Watching" action
 * that deletes the watch-progress entry (self-write; live-updates the Home snapshot, and a
 * router.refresh() covers the server-rendered Library).
 */
export function ContinueWatchingTile({
  item,
  locale,
  className,
}: {
  item: ContinueWatchingItem;
  locale: Locale;
  className?: string;
}) {
  const { movie, progress } = item;
  const { user } = useAuth();
  const { dict } = useI18n();
  const router = useRouter();
  const [menuAt, setMenuAt] = useState<{ top: number; left: number } | null>(null);
  const title = localized(movie.title, locale);

  const openAt = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setMenuAt({ top: e.clientY, left: e.clientX });
  };

  const remove = async () => {
    if (!user) return;
    await deleteDoc(doc(db, "watchProgress", user.uid, "items", movie.id));
    router.refresh();
  };

  return (
    <>
      <div className={cn("group", className)}>
        <div className="relative aspect-[2/3] overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
          <Link href={`/watch/${movie.id}`} onContextMenu={openAt} className="block h-full w-full" aria-label={title}>
            {movie.posterUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={movie.posterUrl}
                alt=""
                loading="lazy"
                className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
              />
            ) : (
              <div className="flex h-full items-center justify-center p-2 text-center text-xs text-ink-faint">{title}</div>
            )}
            <div className="absolute inset-x-0 bottom-0 h-1 bg-black/50">
              <div className="h-full bg-brand-500" style={{ width: `${Math.min(100, Math.max(0, progress * 100))}%` }} />
            </div>
          </Link>

          <button
            type="button"
            aria-label={dict.catalog.movieOptions}
            onClick={openAt}
            className="absolute right-1.5 top-1.5 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-black/55 text-white opacity-0 transition-opacity hover:bg-black/75 focus-visible:opacity-100 group-hover:opacity-100 max-[768px]:opacity-100"
          >
            <MoreVertical className="h-4 w-4" aria-hidden />
          </button>
        </div>

        <Link href={`/watch/${movie.id}`} onContextMenu={openAt} className="mt-1.5 block">
          <p className="truncate text-sm text-ink">{title}</p>
        </Link>
      </div>

      {menuAt && (
        <MovieMenu
          movie={movie}
          locale={locale}
          position={menuAt}
          dict={dict.catalog}
          onClose={() => setMenuAt(null)}
          onRemove={remove}
          removeLabel={dict.catalog.removeFromContinue}
        />
      )}
    </>
  );
}
