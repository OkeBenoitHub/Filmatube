"use client";

import { useEffect, useRef, useState, type ComponentType } from "react";
import { useRouter } from "next/navigation";
import {
  Bookmark,
  BookmarkCheck,
  Check,
  FolderPlus,
  Info,
  Link2,
  Play,
  Share2,
} from "lucide-react";
import {
  collection,
  deleteDoc,
  doc,
  getDocs,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  where,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { addMovieToCollection } from "@/app/collections/actions";
import { useAuth } from "@/components/providers/AuthProvider";
import { localized, type CatalogMovie } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

interface Pos {
  top: number;
  left: number;
}

/**
 * Context menu for a movie thumbnail — opened by the 3-dot button or right-click, positioned at
 * the trigger. Mirrors the Spotitube song menu: a poster/title header, then grouped actions
 * (Play / More info · Watch Later / Add to collection · Copy link / Share). Self-contained —
 * watchlist + collection writes go through the client SDK / the existing server action.
 */
export function MovieMenu({
  movie,
  locale,
  position,
  dict,
  onClose,
}: {
  movie: CatalogMovie;
  locale: Locale;
  position: Pos;
  dict: Dictionary["catalog"];
  onClose: () => void;
}) {
  const { user } = useAuth();
  const router = useRouter();
  const ref = useRef<HTMLDivElement>(null);
  const [coords, setCoords] = useState<Pos>(position);
  const [saved, setSaved] = useState(false);
  const [picker, setPicker] = useState(false);
  const [collections, setCollections] = useState<{ id: string; title: string }[] | null>(null);
  const [addedTo, setAddedTo] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const title = localized(movie.title, locale);

  // Live watchlist state so the item reads Watch Later / Saved correctly.
  useEffect(() => {
    if (!user) return;
    return onSnapshot(doc(db, "watchlists", user.uid, "movies", movie.id), (s) => setSaved(s.exists()));
  }, [user, movie.id]);

  // Clamp to the viewport once measured, so a card near the edge doesn't open off-screen.
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    const pad = 8;
    let top = position.top;
    let left = position.left;
    if (left + r.width > window.innerWidth - pad) left = window.innerWidth - r.width - pad;
    if (top + r.height > window.innerHeight - pad) top = window.innerHeight - r.height - pad;
    setCoords({ top: Math.max(pad, top), left: Math.max(pad, left) });
  }, [position, picker]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    window.addEventListener("scroll", onClose, true);
    return () => {
      document.removeEventListener("keydown", onKey);
      window.removeEventListener("scroll", onClose, true);
    };
  }, [onClose]);

  const go = (href: string) => {
    onClose();
    router.push(href);
  };

  const toggleSaved = async () => {
    if (!user) return;
    const r = doc(db, "watchlists", user.uid, "movies", movie.id);
    if (saved) await deleteDoc(r);
    else await setDoc(r, { movieId: movie.id, addedAt: serverTimestamp() });
  };

  const openPicker = async () => {
    setPicker(true);
    if (collections || !user) return;
    const snap = await getDocs(query(collection(db, "collections"), where("userId", "==", user.uid)));
    setCollections(
      snap.docs
        .map((d) => ({ id: d.id, title: (d.get("title") as string) ?? "" }))
        .sort((a, b) => a.title.localeCompare(b.title)),
    );
  };

  const addTo = async (cid: string) => {
    setAddedTo(cid);
    await addMovieToCollection(cid, movie.id);
  };

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(`${window.location.origin}/movie/${movie.id}`);
      setCopied(true);
      setTimeout(onClose, 900);
    } catch {
      onClose();
    }
  };

  const share = async () => {
    const url = `${window.location.origin}/movie/${movie.id}`;
    if (typeof navigator !== "undefined" && navigator.share) {
      try {
        await navigator.share({ title, url });
      } catch {
        /* user dismissed */
      }
      onClose();
    } else {
      void copyLink();
    }
  };

  return (
    // Full-screen catcher: any click/right-click outside the menu closes it.
    <div
      className="fixed inset-0 z-[60]"
      onMouseDown={onClose}
      onContextMenu={(e) => {
        e.preventDefault();
        onClose();
      }}
    >
      <div
        ref={ref}
        role="menu"
        style={{ top: coords.top, left: coords.left }}
        onMouseDown={(e) => e.stopPropagation()}
        className="absolute w-60 overflow-hidden rounded-xl border border-surface-border bg-surface-card shadow-2xl shadow-black/50"
      >
        {!picker ? (
          <>
            <div className="flex items-center gap-2.5 border-b border-surface-border px-3 py-2.5">
              <div className="h-12 w-9 shrink-0 overflow-hidden rounded bg-surface-hover">
                {movie.posterUrl && (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={movie.posterUrl} alt="" className="h-full w-full object-cover" />
                )}
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-ink">{title}</p>
                {movie.year > 0 && <p className="text-xs text-ink-faint">{movie.year}</p>}
              </div>
            </div>

            <div className="py-1">
              {!movie.isComingSoon && <Item icon={Play} label={dict.play} onClick={() => go(`/watch/${movie.id}`)} />}
              <Item icon={Info} label={dict.moreInfo} onClick={() => go(`/movie/${movie.id}`)} />
              <Divider />
              <Item
                icon={saved ? BookmarkCheck : Bookmark}
                label={saved ? dict.saved : dict.watchLater}
                onClick={toggleSaved}
                accent={saved}
              />
              <Item icon={FolderPlus} label={dict.addToCollection} onClick={openPicker} />
              <Divider />
              <Item icon={copied ? Check : Link2} label={copied ? dict.copied : dict.copyLink} onClick={copyLink} accent={copied} />
              <Item icon={Share2} label={dict.shareAction} onClick={share} />
            </div>
          </>
        ) : (
          <div className="py-1">
            <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-ink-faint">
              {dict.addToCollection}
            </div>
            <div className="max-h-64 overflow-y-auto">
              {collections === null ? (
                <p className="px-3 py-3 text-sm text-ink-muted">{dict.loadingLabel}</p>
              ) : collections.length === 0 ? (
                <p className="px-3 py-3 text-sm text-ink-muted">{dict.noCollections}</p>
              ) : (
                collections.map((col) => (
                  <button
                    key={col.id}
                    type="button"
                    onClick={() => addTo(col.id)}
                    className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left text-sm text-ink transition-colors hover:bg-surface-hover"
                  >
                    <span className="truncate">{col.title}</span>
                    {addedTo === col.id && <Check className="h-4 w-4 shrink-0 text-brand-400" aria-hidden />}
                  </button>
                ))
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function Item({
  icon: Icon,
  label,
  onClick,
  accent,
}: {
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  label: string;
  onClick: () => void;
  accent?: boolean;
}) {
  return (
    <button
      type="button"
      role="menuitem"
      onClick={onClick}
      className="flex w-full items-center gap-3 px-3 py-2 text-left text-sm text-ink transition-colors hover:bg-surface-hover"
    >
      <Icon className={cn("h-4 w-4 shrink-0", accent ? "text-brand-400" : "text-ink-muted")} aria-hidden />
      {label}
    </button>
  );
}

function Divider() {
  return <div className="my-1 h-px bg-surface-border" />;
}
