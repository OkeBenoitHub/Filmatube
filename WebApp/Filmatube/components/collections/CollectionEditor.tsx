"use client";

import { useRef, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Upload, Search, X, Plus, Share2, Check, Bookmark, ChevronLeft, ChevronRight } from "lucide-react";
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
import { Input } from "@/components/ui/Input";
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

  return (
    <div className="mx-auto max-w-4xl space-y-6 px-4 py-8 md:px-6">
      {isOwner ? (
        <>
          <div className="flex flex-col gap-4 sm:flex-row">
            <button
              type="button"
              onClick={() => coverInput.current?.click()}
              className="relative aspect-video w-full shrink-0 overflow-hidden rounded-lg border border-surface-border bg-surface-hover sm:w-64"
            >
              {coverUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={coverUrl} alt="" className="h-full w-full object-cover" />
              ) : (
                <span className="flex h-full items-center justify-center gap-2 text-sm text-ink-muted">
                  <Upload className="h-4 w-4" aria-hidden />
                  {dict.cover}
                </span>
              )}
            </button>
            <input ref={coverInput} type="file" accept="image/*" onChange={onCover} className="hidden" />

            <div className="flex-1 space-y-3">
              <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder={dict.collectionTitle} />
              <label className="flex items-center gap-2 text-sm text-ink">
                <input type="checkbox" checked={isPublic} onChange={(e) => setIsPublic(e.target.checked)} className="h-4 w-4 accent-brand-500" />
                {dict.makePublic}
              </label>
              <div className="flex gap-2">
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
                {ShareButton}
              </div>
            </div>
          </div>

          {/* Add movies */}
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
        </>
      ) : (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-ink">{collection.title}</h1>
          <div className="flex gap-2">
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
          </div>
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
