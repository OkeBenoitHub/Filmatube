"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { upsertMovie } from "@/app/admin/movies/actions";
import { GENRE_KEYS } from "@/lib/genres";
import type { MovieFormValues } from "@/lib/admin/movie-form";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Chip } from "@/components/account/Chip";
import { ErrorBanner } from "@/components/auth/AuthBits";

export function MovieForm({
  movieId,
  initial,
  dict,
  genres,
}: {
  movieId: string | null;
  initial: MovieFormValues;
  dict: Dictionary["adminMovies"];
  genres: Dictionary["genres"];
}) {
  const router = useRouter();
  const [values, setValues] = useState<MovieFormValues>(initial);
  const [directorsText, setDirectorsText] = useState(initial.directors.join(", "));
  const [tmdbInput, setTmdbInput] = useState(initial.tmdbId || initial.imdbId);
  const [autofilling, setAutofilling] = useState(false);
  const [autofillError, setAutofillError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [titleError, setTitleError] = useState(false);

  const set = (patch: Partial<MovieFormValues>) => setValues((v) => ({ ...v, ...patch }));

  async function autofill() {
    if (!tmdbInput.trim()) return;
    setAutofilling(true);
    setAutofillError(false);
    try {
      const res = await fetch("/api/admin/tmdb", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id: tmdbInput.trim() }),
      });
      if (!res.ok) throw new Error("tmdb");
      const data = (await res.json()) as Partial<MovieFormValues>;
      setValues((v) => ({ ...v, ...data }));
      if (data.directors) setDirectorsText(data.directors.join(", "));
    } catch {
      setAutofillError(true);
    } finally {
      setAutofilling(false);
    }
  }

  function toggleGenre(key: string) {
    set({ genres: values.genres.includes(key) ? values.genres.filter((g) => g !== key) : [...values.genres, key] });
  }

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!values.titleEn.trim()) {
      setTitleError(true);
      return;
    }
    setSaving(true);
    try {
      const directors = directorsText.split(",").map((s) => s.trim()).filter(Boolean);
      await upsertMovie(movieId, { ...values, directors });
      router.push("/admin/movies");
      router.refresh();
    } catch {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={save} className="max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-ink">{movieId ? dict.editTitle : dict.newTitle}</h1>

      {/* TMDB auto-fill */}
      <div className="space-y-2">
        <label className="text-sm font-medium text-ink">{dict.idLabel}</label>
        <div className="flex gap-2">
          <Input placeholder={dict.idHint} value={tmdbInput} onChange={(e) => setTmdbInput(e.target.value)} />
          <Button type="button" variant="secondary" onClick={autofill} loading={autofilling}>
            {dict.autofill}
          </Button>
        </div>
        {autofillError && <ErrorBanner>{dict.autofillError}</ErrorBanner>}
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Field label={dict.titleEn} error={titleError ? dict.requiredTitle : undefined}>
          <Input value={values.titleEn} onChange={(e) => { set({ titleEn: e.target.value }); setTitleError(false); }} />
        </Field>
        <Field label={dict.titleFr}>
          <Input value={values.titleFr} onChange={(e) => set({ titleFr: e.target.value })} />
        </Field>
      </div>

      <Field label={dict.descEn}>
        <Textarea value={values.descEn} onChange={(e) => set({ descEn: e.target.value })} />
      </Field>
      <Field label={dict.descFr}>
        <Textarea value={values.descFr} onChange={(e) => set({ descFr: e.target.value })} />
      </Field>

      <div className="grid gap-3 sm:grid-cols-3">
        <Field label={dict.year}>
          <Input type="number" value={values.year} onChange={(e) => set({ year: Number(e.target.value) })} />
        </Field>
        <Field label={dict.duration}>
          <Input type="number" value={values.duration} onChange={(e) => set({ duration: Number(e.target.value) })} />
        </Field>
        <Field label={dict.ageRating}>
          <Input value={values.ageRating} onChange={(e) => set({ ageRating: e.target.value })} />
        </Field>
      </div>

      <Field label={dict.genres}>
        <div className="flex flex-wrap gap-2">
          {GENRE_KEYS.map((key) => (
            <Chip key={key} label={genres[key]} selected={values.genres.includes(key)} onClick={() => toggleGenre(key)} />
          ))}
        </div>
      </Field>

      <Field label={dict.directors}>
        <Input value={directorsText} onChange={(e) => setDirectorsText(e.target.value)} />
      </Field>

      {values.cast.length > 0 && (
        <Field label={dict.cast}>
          <div className="flex flex-wrap gap-1.5">
            {values.cast.map((c) => (
              <span key={c.name} className="rounded-full bg-surface-hover px-2.5 py-1 text-xs text-ink-muted">
                {c.name}
              </span>
            ))}
          </div>
        </Field>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        <Field label={dict.posterUrl}>
          <Input value={values.posterUrl} onChange={(e) => set({ posterUrl: e.target.value })} />
        </Field>
        <Field label={dict.backdropUrl}>
          <Input value={values.backdropUrl} onChange={(e) => set({ backdropUrl: e.target.value })} />
        </Field>
      </div>
      {(values.posterUrl || values.backdropUrl) && (
        <div className="flex gap-3">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          {values.posterUrl && <img src={values.posterUrl} alt="" className="h-32 w-auto rounded-lg object-cover" />}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          {values.backdropUrl && <img src={values.backdropUrl} alt="" className="h-32 w-auto rounded-lg object-cover" />}
        </div>
      )}

      <Field label={dict.trailerUrl}>
        <Input value={values.trailerUrl} onChange={(e) => set({ trailerUrl: e.target.value })} />
      </Field>

      {/* Flags + status */}
      <div className="flex flex-wrap items-center gap-4">
        <Toggle label={dict.featured} checked={values.isFeatured} onChange={(c) => set({ isFeatured: c })} />
        <Toggle label={dict.pinned} checked={values.isPinned} onChange={(c) => set({ isPinned: c })} />
        <Toggle label={dict.comingSoon} checked={values.isComingSoon} onChange={(c) => set({ isComingSoon: c })} />
        <select
          value={values.status}
          onChange={(e) => set({ status: e.target.value as "draft" | "published" })}
          className="h-10 rounded-lg border border-surface-border bg-surface px-3 text-sm text-ink"
        >
          <option value="draft">{dict.draft}</option>
          <option value="published">{dict.published}</option>
        </select>
      </div>

      <div className="flex gap-2">
        <Button type="submit" loading={saving}>{dict.save}</Button>
        <Button type="button" variant="outline" onClick={() => router.push("/admin/movies")}>
          {dict.cancel}
        </Button>
      </div>
    </form>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium text-ink">{label}</label>
      {children}
      {error && <p className="text-xs text-red-300">{error}</p>}
    </div>
  );
}

function Textarea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      rows={3}
      className="w-full rounded-lg border border-surface-border bg-surface px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
      {...props}
    />
  );
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (c: boolean) => void }) {
  return (
    <label className="flex items-center gap-2 text-sm text-ink">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} className="h-4 w-4 accent-brand-500" />
      {label}
    </label>
  );
}
