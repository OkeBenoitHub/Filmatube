"use client";

import { useState } from "react";
import { ArrowDown, ArrowUp, Ban, Check, Pencil, Pin, Plus, Search, Trash2, X } from "lucide-react";
import { Card } from "@/components/ui/Card";
import { useI18n } from "@/components/providers/LocaleProvider";
import type { HomeRow, MovieBrief } from "@/lib/admin/home-rows";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import {
  deleteHomeRow,
  reorderHomeRows,
  setHomeRowEnabled,
  upsertHomeRow,
  type HomeRowInput,
} from "@/app/admin/curation/actions";

type Dict = Dictionary["adminCuration"];

/** Epoch millis → value for <input type="datetime-local">, in local time. */
function toLocalInput(ms: number | null): string {
  if (ms == null) return "";
  const d = new Date(ms - new Date(ms).getTimezoneOffset() * 60000);
  return d.toISOString().slice(0, 16);
}
const fromLocalInput = (v: string): number | null => (v ? new Date(v).getTime() : null);

const blankDraft = (): HomeRow => ({
  id: "",
  titleEn: "",
  titleFr: "",
  movieIds: [],
  enabled: true,
  pinned: true,
  order: 0,
  startAtMs: null,
  endAtMs: null,
  movies: [],
});

export function CurationManager({ rows, dict }: { rows: HomeRow[]; dict: Dict }) {
  const [editing, setEditing] = useState<HomeRow | null>(null);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-ink">{dict.title}</h1>
          <p className="mt-1 text-sm text-ink-muted">{dict.subtitle}</p>
        </div>
        <button
          type="button"
          onClick={() => setEditing(blankDraft())}
          className="inline-flex h-9 shrink-0 items-center gap-2 rounded-lg bg-brand-600 px-3 text-sm font-semibold text-white hover:bg-brand-500"
        >
          <Plus className="h-4 w-4" aria-hidden />
          {dict.newRow}
        </button>
      </div>

      {rows.length === 0 ? (
        <Card className="p-8 text-center text-ink-muted">{dict.empty}</Card>
      ) : (
        <RowList rows={rows} dict={dict} onEdit={setEditing} />
      )}

      {editing && <RowEditor key={editing.id || "new"} initial={editing} dict={dict} onClose={() => setEditing(null)} />}
    </div>
  );
}

function statusOf(row: HomeRow, dict: Dict): { label: string; tone: string } {
  if (!row.enabled) return { label: dict.statusDisabled, tone: "text-ink-faint" };
  const now = Date.now();
  if (row.startAtMs && now < row.startAtMs) return { label: dict.statusScheduled, tone: "text-amber-400" };
  if (row.endAtMs && now > row.endAtMs) return { label: dict.statusExpired, tone: "text-ink-faint" };
  return { label: dict.statusLive, tone: "text-brand-400" };
}

function RowList({
  rows,
  dict,
  onEdit,
}: {
  rows: HomeRow[];
  dict: Dict;
  onEdit: (r: HomeRow) => void;
}) {
  const [busy, setBusy] = useState(false);

  const move = async (i: number, dir: -1 | 1) => {
    const j = i + dir;
    if (j < 0 || j >= rows.length) return;
    const ids = rows.map((r) => r.id);
    [ids[i], ids[j]] = [ids[j], ids[i]];
    setBusy(true);
    await reorderHomeRows(ids);
    setBusy(false);
  };

  return (
    <div className="space-y-2">
      {rows.map((row, i) => {
        const st = statusOf(row, dict);
        return (
          <Card key={row.id} className="flex items-center gap-3 p-3">
            <div className="flex flex-col">
              <button type="button" disabled={busy || i === 0} onClick={() => move(i, -1)} className="text-ink-faint hover:text-ink disabled:opacity-30" aria-label={dict.moveUp}>
                <ArrowUp className="h-4 w-4" />
              </button>
              <button type="button" disabled={busy || i === rows.length - 1} onClick={() => move(i, 1)} className="text-ink-faint hover:text-ink disabled:opacity-30" aria-label={dict.moveDown}>
                <ArrowDown className="h-4 w-4" />
              </button>
            </div>

            {/* Poster stack preview. */}
            <div className="flex -space-x-3">
              {row.movies.slice(0, 5).map((m) => (
                // eslint-disable-next-line @next/next/no-img-element
                <img key={m.id} src={m.posterUrl} alt="" className="h-14 w-10 rounded border border-surface-border object-cover" />
              ))}
              {row.movies.length === 0 && <div className="h-14 w-10 rounded border border-dashed border-surface-border" />}
            </div>

            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                {row.pinned && <Pin className="h-3.5 w-3.5 shrink-0 text-brand-400" aria-label={dict.pinned} />}
                <p className="truncate font-semibold text-ink">{row.titleEn || dict.untitled}</p>
              </div>
              <p className="mt-0.5 text-xs text-ink-muted">
                {dict.movieCount.replace("{n}", String(row.movies.length))}
                {" · "}
                <span className={st.tone}>{st.label}</span>
              </p>
            </div>

            <button
              type="button"
              onClick={async () => { await setHomeRowEnabled(row.id, !row.enabled); }}
              className="rounded-lg border border-surface-border px-2.5 py-1.5 text-xs font-medium text-ink hover:bg-surface-hover"
            >
              {row.enabled ? dict.disable : dict.enable}
            </button>
            <button type="button" onClick={() => onEdit(row)} className="rounded-lg border border-surface-border p-2 text-ink hover:bg-surface-hover" aria-label={dict.edit}>
              <Pencil className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={async () => { if (confirm(dict.confirmDelete)) await deleteHomeRow(row.id); }}
              className="rounded-lg border border-surface-border p-2 text-red-400 hover:bg-red-500/10"
              aria-label={dict.delete}
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </Card>
        );
      })}
    </div>
  );
}

function RowEditor({ initial, dict, onClose }: { initial: HomeRow; dict: Dict; onClose: () => void }) {
  const { locale } = useI18n();
  const [titleEn, setTitleEn] = useState(initial.titleEn);
  const [titleFr, setTitleFr] = useState(initial.titleFr);
  const [enabled, setEnabled] = useState(initial.enabled);
  const [pinned, setPinned] = useState(initial.pinned);
  const [startAt, setStartAt] = useState(toLocalInput(initial.startAtMs));
  const [endAt, setEndAt] = useState(toLocalInput(initial.endAtMs));
  const [selected, setSelected] = useState<MovieBrief[]>(initial.movies);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const save = async () => {
    if (!titleEn.trim()) { setError(dict.errTitle); return; }
    if (selected.length === 0) { setError(dict.errMovies); return; }
    const payload: HomeRowInput = {
      titleEn,
      titleFr: titleFr.trim() || titleEn,
      movieIds: selected.map((m) => m.id),
      enabled,
      pinned,
      startAtMs: fromLocalInput(startAt),
      endAtMs: fromLocalInput(endAt),
    };
    setSaving(true);
    try {
      await upsertHomeRow(initial.id || null, payload);
      onClose();
    } catch {
      setError(dict.errSave);
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/60 p-4" onMouseDown={onClose}>
      <div className="mt-8 w-full max-w-2xl rounded-2xl border border-surface-border bg-surface-card p-5 shadow-2xl" onMouseDown={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-bold text-ink">{initial.id ? dict.editRow : dict.newRow}</h2>
          <button type="button" onClick={onClose} className="text-ink-muted hover:text-ink" aria-label={dict.close}><X className="h-5 w-5" /></button>
        </div>

        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label={dict.titleEnLabel}>
              <input value={titleEn} onChange={(e) => setTitleEn(e.target.value)} placeholder={dict.titlePlaceholder} className={inputClass} />
            </Field>
            <Field label={dict.titleFrLabel}>
              <input value={titleFr} onChange={(e) => setTitleFr(e.target.value)} placeholder={dict.titleFrPlaceholder} className={inputClass} />
            </Field>
          </div>

          <div className="flex flex-wrap gap-2">
            <Toggle on={pinned} onClick={() => setPinned((v) => !v)} icon={Pin} label={dict.pinAbove} />
            <Toggle on={enabled} onClick={() => setEnabled((v) => !v)} icon={enabled ? Check : Ban} label={enabled ? dict.enabledLabel : dict.disabledLabel} />
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <Field label={dict.startAtLabel}>
              <input type="datetime-local" value={startAt} onChange={(e) => setStartAt(e.target.value)} className={inputClass} />
            </Field>
            <Field label={dict.endAtLabel}>
              <input type="datetime-local" value={endAt} onChange={(e) => setEndAt(e.target.value)} className={inputClass} />
            </Field>
          </div>
          <p className="-mt-2 text-xs text-ink-faint">{dict.scheduleHint}</p>

          <MoviePicker dict={dict} locale={locale} selected={selected} onChange={setSelected} />

          {error && <p className="text-sm text-red-400">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="h-9 rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover">{dict.cancel}</button>
            <button type="button" disabled={saving} onClick={save} className="h-9 rounded-lg bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-500 disabled:opacity-50">
              {saving ? dict.saving : dict.save}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function MoviePicker({
  dict,
  locale,
  selected,
  onChange,
}: {
  dict: Dict;
  locale: string;
  selected: MovieBrief[];
  onChange: (m: MovieBrief[]) => void;
}) {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<MovieBrief[]>([]);
  const [loading, setLoading] = useState(false);

  const runSearch = async (term: string) => {
    setQ(term);
    if (!term.trim()) { setResults([]); return; }
    setLoading(true);
    try {
      const res = await fetch(`/api/movies/search?q=${encodeURIComponent(term)}`);
      const data = (await res.json()) as { results?: { id: string; title: string; posterUrl: string; year: number }[] };
      setResults((data.results ?? []).map((r) => ({ id: r.id, title: r.title, posterUrl: r.posterUrl, year: r.year })));
    } catch {
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const selectedIds = new Set(selected.map((m) => m.id));
  const add = (m: MovieBrief) => { if (!selectedIds.has(m.id)) onChange([...selected, m]); };
  const remove = (id: string) => onChange(selected.filter((m) => m.id !== id));
  const move = (i: number, dir: -1 | 1) => {
    const j = i + dir;
    if (j < 0 || j >= selected.length) return;
    const next = [...selected];
    [next[i], next[j]] = [next[j], next[i]];
    onChange(next);
  };

  return (
    <div className="space-y-2">
      <p className="text-xs font-semibold uppercase tracking-wide text-ink-faint">
        {dict.moviesLabel} · {dict.movieCount.replace("{n}", String(selected.length))}
      </p>

      {/* Selected, ordered — this is the row exactly as it will appear on Home. */}
      {selected.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {selected.map((m, i) => (
            <div key={m.id} className="flex items-center gap-1.5 rounded-lg border border-surface-border bg-surface py-1 pl-1 pr-2">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={m.posterUrl} alt="" className="h-9 w-6 rounded object-cover" />
              <span className="max-w-[9rem] truncate text-xs text-ink" lang={locale}>{m.title}</span>
              <button type="button" onClick={() => move(i, -1)} disabled={i === 0} className="text-ink-faint hover:text-ink disabled:opacity-30" aria-label={dict.moveUp}><ArrowUp className="h-3.5 w-3.5" /></button>
              <button type="button" onClick={() => move(i, 1)} disabled={i === selected.length - 1} className="text-ink-faint hover:text-ink disabled:opacity-30" aria-label={dict.moveDown}><ArrowDown className="h-3.5 w-3.5" /></button>
              <button type="button" onClick={() => remove(m.id)} className="text-red-400 hover:text-red-300" aria-label={dict.remove}><X className="h-3.5 w-3.5" /></button>
            </div>
          ))}
        </div>
      )}

      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" aria-hidden />
        <input value={q} onChange={(e) => runSearch(e.target.value)} placeholder={dict.searchPlaceholder} className={`${inputClass} pl-9`} />
      </div>

      {q.trim() && (
        <div className="max-h-52 space-y-1 overflow-y-auto rounded-lg border border-surface-border p-1">
          {loading && results.length === 0 ? (
            <p className="px-2 py-3 text-sm text-ink-muted">…</p>
          ) : results.length === 0 ? (
            <p className="px-2 py-3 text-sm text-ink-muted">{dict.noResults}</p>
          ) : (
            results.map((m) => (
              <button
                key={m.id}
                type="button"
                onClick={() => add(m)}
                disabled={selectedIds.has(m.id)}
                className="flex w-full items-center gap-2 rounded px-2 py-1.5 text-left hover:bg-surface-hover disabled:opacity-40"
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={m.posterUrl} alt="" className="h-10 w-7 rounded object-cover" />
                <span className="flex-1 truncate text-sm text-ink">{m.title}</span>
                {m.year > 0 && <span className="text-xs text-ink-faint">{m.year}</span>}
                {selectedIds.has(m.id) ? <Check className="h-4 w-4 text-brand-400" /> : <Plus className="h-4 w-4 text-ink-muted" />}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}

const inputClass =
  "h-9 w-full rounded-lg border border-surface-border bg-surface px-3 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-400";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block space-y-1">
      <span className="text-xs font-medium text-ink-muted">{label}</span>
      {children}
    </label>
  );
}

function Toggle({ on, onClick, icon: Icon, label }: { on: boolean; onClick: () => void; icon: typeof Pin; label: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex h-8 items-center gap-1.5 rounded-lg border px-3 text-[13px] font-medium transition-colors ${
        on ? "border-brand-500 bg-brand-600/20 text-brand-300" : "border-surface-border text-ink-muted hover:bg-surface-hover"
      }`}
    >
      <Icon className="h-3.5 w-3.5" aria-hidden />
      {label}
    </button>
  );
}
