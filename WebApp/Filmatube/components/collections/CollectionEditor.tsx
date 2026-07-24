"use client";

import { useRef, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Upload, Search, X, Plus, Share2, Check, Bookmark, ChevronLeft, ChevronRight, Globe, Lock } from "lucide-react";
import { CollectionCover } from "@/components/collections/CollectionCover";
import {
  saveCollection,
  deleteCollection,
  addMovieToCollection,
  removeMovieFromCollection,
  saveCollectionCopy,
  moveCollectionItem,
} from "@/app/collections/actions";
import { uploadPublic } from "@/lib/upload/media";
import type { Collection } from "@/lib/collections";
import type { CatalogMovie } from "@/lib/movies";
import type { Locale } from "@/lib/i18n/config";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { Button } from "@/components/ui/Button";

interface SearchResult {
  id: string;
  title: string;
  posterUrl: string;
}

export function CollectionEditor({
  collection,
  movies,
  locale,
  dict,
  isOwner,
}: {
  collection: Collection;
  movies: CatalogMovie[];
  locale: Locale;
  dict: Dictionary["catalog"];
  isOwner: boolean;
}) {
  const router = useRouter();
  const [title, setTitle] = useState(collection.title);
  const [coverUrl, setCoverUrl] = useState(collection.coverUrl);
  const [isPublic, setIsPublic] = useState(collection.isPublic);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);
  const [pending, startTransition] = useTransition();
  const [copied, setCopied] = useState(false);
  const coverInput = useRef<HTMLInputElement>(null);

  const localized = (m: CatalogMovie) => (locale === "fr" ? m.title.fr || m.title.en : m.title.en || m.title.fr);

  const share = async () => {
    const url = `${window.location.origin}/collections/${collection.id}`;
    if (navigator.share) {
      try {
        await navigator.share({ title: collection.title, url });
        return;
      } catch {
        /* fall through */
      }
    }
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* ignore */
    }
  };

  const ShareButton = (
    <Button variant="outline" onClick={share}>
      {copied ? <Check className="h-4 w-4 text-brand-400" aria-hidden /> : <Share2 className="h-4 w-4" aria-hidden />}
      {copied ? dict.copied : dict.share}
    </Button>
  );

  const onCover = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    const url = await uploadPublic(file, "avatars");
    setCoverUrl(url);
  };

  const search = async (q: string) => {
    setQuery(q);
    if (!q.trim()) {
      setResults([]);
      return;
    }
    try {
      const res = await fetch(`/api/movies/search?q=${encodeURIComponent(q)}`);
      const data = (await res.json()) as { results?: SearchResult[] };
      setResults(data.results ?? []);
    } catch {
      setResults([]);
    }
  };

  const inCollection = new Set(movies.map((m) => m.id));
  const Visibility = isPublic ? Globe : Lock;
  const visibilityLabel = isPublic ? dict.makePublic : dict.collectionPrivateShort;

  return (
    <div className="mx-auto max-w-4xl space-y-8 px-4 py-8 md:px-6">
      {/* Hero — cover image with the title to its left/right, matching the app's page pattern. */}
      <div className="flex flex-col gap-6 sm:flex-row sm:items-end">
        {isOwner ? (
          <button
            type="button"
            onClick={() => coverInput.current?.click()}
            className="group relative aspect-video w-full shrink-0 overflow-hidden rounded-2xl border border-surface-border shadow-xl shadow-black/30 sm:w-72"
          >
            <CollectionCover coverUrl={coverUrl} title={title} />
            <span className="absolute inset-0 flex items-center justify-center gap-2 bg-black/45 text-sm font-medium text-white opacity-0 transition-opacity group-hover:opacity-100">
              <Upload className="h-4 w-4" aria-hidden />
              {dict.cover}
            </span>
          </button>
        ) : (
          <div className="aspect-video w-full shrink-0 overflow-hidden rounded-2xl border border-surface-border shadow-xl shadow-black/30 sm:w-72">
            <CollectionCover coverUrl={collection.coverUrl} title={collection.title} />
          </div>
        )}
        <input ref={coverInput} type="file" accept="image/*" onChange={onCover} className="hidden" />

        <div className="min-w-0 flex-1 space-y-2.5 text-center sm:text-left">
          <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{dict.collectionEyebrow}</p>
          {isOwner ? (
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder={dict.collectionTitle}
              className="w-full border-b border-transparent bg-transparent text-center text-3xl font-black tracking-tight text-ink outline-none transition-colors placeholder:text-ink-faint focus:border-brand-500 sm:text-left md:text-4xl"
            />
          ) : (
            <h1 className="text-3xl font-black tracking-tight text-ink md:text-4xl">{collection.title}</h1>
          )}

          <div className="flex flex-wrap items-center justify-center gap-x-2 gap-y-1 text-sm text-ink-muted sm:justify-start">
            {isOwner ? (
              <button
                type="button"
                onClick={() => setIsPublic((v) => !v)}
                className="inline-flex items-center gap-1.5 rounded-full border border-surface-border px-2.5 py-0.5 font-medium text-ink transition-colors hover:bg-surface-hover"
              >
                <Visibility className={`h-3.5 w-3.5 ${isPublic ? "text-brand-400" : ""}`} aria-hidden />
                {visibilityLabel}
              </button>
            ) : (
              <span className="inline-flex items-center gap-1.5">
                <Visibility className={`h-3.5 w-3.5 ${collection.isPublic ? "text-brand-400" : ""}`} aria-hidden />
                {collection.isPublic ? dict.makePublic : dict.collectionPrivateShort}
              </span>
            )}
            <span aria-hidden>·</span>
            <span>{dict.collectionMovieCount.replace("{n}", String(movies.length))}</span>
          </div>

          <div className="flex flex-wrap justify-center gap-2 pt-1 sm:justify-start">
            {isOwner ? (
              <>
                <Button
                  loading={pending}
                  onClick={() =>
                    startTransition(async () => {
                      await saveCollection(collection.id, { title, coverUrl, isPublic });
                      router.refresh();
                    })
                  }
                >
                  {dict.save}
                </Button>
                {ShareButton}
                <Button
                  variant="outline"
                  onClick={() =>
                    startTransition(async () => {
                      await deleteCollection(collection.id);
                      router.push("/collections");
                    })
                  }
                >
                  {dict.deleteLabel}
                </Button>
              </>
            ) : (
              <>
                <Button
                  loading={pending}
                  onClick={() =>
                    startTransition(async () => {
                      const newId = await saveCollectionCopy(collection.id);
                      router.push(`/collections/${newId}`);
                    })
                  }
                >
                  <Bookmark className="h-4 w-4" aria-hidden />
                  {dict.saveCopy}
                </Button>
                {ShareButton}
              </>
            )}
          </div>
        </div>
      </div>

      {/* Add movies (owner) */}
      {isOwner && (
        <div className="space-y-3">
          <div className="relative max-w-md">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" aria-hidden />
            <input
              type="search"
              value={query}
              onChange={(e) => search(e.target.value)}
              placeholder={dict.addMovies}
              className="h-10 w-full rounded-lg border border-surface-border bg-surface pl-10 pr-3 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
            />
          </div>
          {results.length > 0 && (
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-5">
              {results.map((r) => (
                <button
                  key={r.id}
                  type="button"
                  disabled={inCollection.has(r.id)}
                  onClick={() =>
                    startTransition(async () => {
                      await addMovieToCollection(collection.id, r.id);
                      setQuery("");
                      setResults([]);
                      router.refresh();
                    })
                  }
                  className="group relative block overflow-hidden rounded-lg border border-surface-border disabled:opacity-40"
                >
                  <div className="aspect-[2/3] bg-surface-hover">
                    {r.posterUrl && (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={r.posterUrl} alt="" className="h-full w-full object-cover" />
                    )}
                  </div>
                  {!inCollection.has(r.id) && (
                    <span className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition-opacity group-hover:opacity-100">
                      <Plus className="h-6 w-6 text-white" aria-hidden />
                    </span>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {movies.length === 0 ? (
        <p className="py-12 text-center text-ink-muted">{dict.collectionEmpty}</p>
      ) : (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {movies.map((movie) => (
            <div key={movie.id} className="group relative">
              <Link href={`/movie/${movie.id}`} className="block">
                <div className="aspect-[2/3] overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
                  {movie.posterUrl && (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={movie.posterUrl} alt="" className="h-full w-full object-cover" />
                  )}
                </div>
                <p className="mt-1.5 truncate text-sm text-ink">{localized(movie)}</p>
              </Link>
              {isOwner && (
                <>
                  <button
                    type="button"
                    aria-label={dict.remove}
                    onClick={() =>
                      startTransition(async () => {
                        await removeMovieFromCollection(collection.id, movie.id);
                        router.refresh();
                      })
                    }
                    className="absolute right-1.5 top-1.5 rounded-full bg-black/60 p-1 text-white opacity-0 transition-opacity hover:bg-black/80 group-hover:opacity-100"
                  >
                    <X className="h-4 w-4" />
                  </button>
                  <div className="absolute bottom-8 left-1.5 flex gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                    <button
                      type="button"
                      aria-label={dict.moveUp}
                      onClick={() => startTransition(async () => { await moveCollectionItem(collection.id, movie.id, "up"); router.refresh(); })}
                      className="rounded-full bg-black/60 p-1 text-white hover:bg-black/80"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      aria-label={dict.moveDown}
                      onClick={() => startTransition(async () => { await moveCollectionItem(collection.id, movie.id, "down"); router.refresh(); })}
                      className="rounded-full bg-black/60 p-1 text-white hover:bg-black/80"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
