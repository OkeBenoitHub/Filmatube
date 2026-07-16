"use client";

import { useRef, useState, useTransition } from "react";
import { Upload } from "lucide-react";
import { createBoard } from "@/app/boards/actions";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { uploadPublic } from "@/lib/upload/media";
import { cn } from "@/lib/utils";

const TYPES = { MOVIE: "movie", GENERAL: "general" } as const;

/** Create-board form. The action writes the doc and redirects server-side (one round-trip). */
export function BoardForm({ dict }: { dict: Dictionary["catalog"] }) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [type, setType] = useState<string>(TYPES.GENERAL);
  const [isPublic, setIsPublic] = useState(true);
  const [coverUrl, setCoverUrl] = useState("");
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const [pending, startTransition] = useTransition();
  const coverInput = useRef<HTMLInputElement>(null);

  const onCover = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setUploading(true);
    setError("");
    try {
      setCoverUrl(await uploadPublic(file, "avatars"));
    } catch {
      setError(dict.boardCreateError);
    } finally {
      setUploading(false);
    }
  };

  const submit = () => {
    if (!title.trim()) {
      setError(dict.boardTitleRequired);
      return;
    }
    setError("");
    startTransition(async () => {
      try {
        await createBoard({ title, description, type, isPublic, coverUrl });
      } catch (e) {
        // redirect() throws NEXT_REDIRECT — that's the success path, not an error.
        if (e instanceof Error && e.message === "NEXT_REDIRECT") throw e;
        if (typeof e === "object" && e !== null && "digest" in e) throw e;
        setError(dict.boardCreateError);
      }
    });
  };

  const typeOptions = [
    { value: TYPES.GENERAL, label: dict.boardTypeGeneral },
    { value: TYPES.MOVIE, label: dict.boardTypeMovie },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-4 sm:flex-row">
        <button
          type="button"
          onClick={() => coverInput.current?.click()}
          disabled={uploading}
          className="relative aspect-video w-full shrink-0 overflow-hidden rounded-xl border border-surface-border bg-surface-hover disabled:opacity-60 sm:w-64"
        >
          {coverUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={coverUrl} alt="" className="h-full w-full object-cover" />
          ) : (
            <span className="flex h-full items-center justify-center gap-2 text-sm text-ink-muted">
              <Upload className="h-4 w-4" aria-hidden />
              {dict.boardCover}
            </span>
          )}
        </button>
        <input ref={coverInput} type="file" accept="image/*" onChange={onCover} className="hidden" />

        <div className="flex-1 space-y-3">
          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
              {dict.boardTitle}
            </span>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder={dict.boardTitlePlaceholder} />
          </label>
          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
              {dict.boardDescription}
            </span>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={dict.boardDescriptionPlaceholder}
              rows={3}
              className="w-full rounded-lg border border-surface-border bg-surface px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
            />
          </label>
        </div>
      </div>

      <div>
        <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
          {dict.boardType}
        </span>
        <div className="flex flex-wrap gap-2">
          {typeOptions.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => setType(t.value)}
              aria-pressed={type === t.value}
              className={cn(
                "rounded-full border px-4 py-1.5 text-sm font-semibold transition-colors",
                type === t.value
                  ? "border-brand-500 bg-brand-500 text-white"
                  : "border-surface-border text-ink-muted hover:bg-surface-hover hover:text-ink",
              )}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <label className="flex items-center gap-2 text-sm text-ink">
        <input
          type="checkbox"
          checked={isPublic}
          onChange={(e) => setIsPublic(e.target.checked)}
          className="h-4 w-4 accent-brand-500"
        />
        {dict.boardPublic}
      </label>

      {error && <p className="text-sm text-red-400">{error}</p>}

      <Button onClick={submit} loading={pending || uploading}>
        {dict.boardCreate}
      </Button>
    </div>
  );
}
