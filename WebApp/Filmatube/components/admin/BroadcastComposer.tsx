"use client";

import { useState, useTransition } from "react";
import { Send, Eye } from "lucide-react";
import { createBroadcast, previewRecipients, type BroadcastInput } from "@/app/admin/notifications/actions";
import { GENRE_KEYS } from "@/lib/genres";
import type { Segment } from "@/lib/admin/broadcast";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export function BroadcastComposer({
  dict,
  genreLabels,
}: {
  dict: Dictionary["admin"];
  genreLabels: Record<string, string>;
}) {
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [segment, setSegment] = useState<Segment>("all");
  const [genre, setGenre] = useState<string>(GENRE_KEYS[0]);
  const [movieId, setMovieId] = useState("");
  const [scheduledAt, setScheduledAt] = useState("");
  const [recipients, setRecipients] = useState<number | null>(null);
  const [result, setResult] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  const preview = () => {
    startTransition(async () => {
      setResult(null);
      setRecipients(await previewRecipients(segment, genre));
    });
  };

  const send = () => {
    const input: BroadcastInput = { title, body, segment, genre, movieId, scheduledAt };
    startTransition(async () => {
      const res = await createBroadcast(input);
      setResult(res.sent ? `${dict.broadcastSent} · ${res.delivered}` : dict.broadcastScheduled);
      setTitle("");
      setBody("");
      setMovieId("");
      setScheduledAt("");
      setRecipients(null);
    });
  };

  const inputCls =
    "w-full rounded-lg border border-surface-border bg-surface-hover px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-brand-500 focus:outline-none";

  return (
    <div className="max-w-xl space-y-4">
      <div className="space-y-1">
        <label className="text-sm font-medium text-ink">{dict.broadcastTitleLabel}</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} className={inputCls} />
      </div>
      <div className="space-y-1">
        <label className="text-sm font-medium text-ink">{dict.broadcastBodyLabel}</label>
        <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={3} className={`${inputCls} resize-none`} />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1">
          <label className="text-sm font-medium text-ink">{dict.broadcastSegment}</label>
          <select value={segment} onChange={(e) => setSegment(e.target.value as Segment)} className={inputCls}>
            <option value="all">{dict.segmentAll}</option>
            <option value="taste">{dict.segmentTaste}</option>
            <option value="active">{dict.segmentActive}</option>
          </select>
        </div>
        {segment === "taste" && (
          <div className="space-y-1">
            <label className="text-sm font-medium text-ink">{dict.broadcastGenre}</label>
            <select value={genre} onChange={(e) => setGenre(e.target.value)} className={inputCls}>
              {GENRE_KEYS.map((g) => (
                <option key={g} value={g}>
                  {genreLabels[g] ?? g}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1">
          <label className="text-sm font-medium text-ink">{dict.broadcastMovieId}</label>
          <input value={movieId} onChange={(e) => setMovieId(e.target.value)} className={inputCls} />
        </div>
        <div className="space-y-1">
          <label className="text-sm font-medium text-ink">{dict.broadcastSchedule}</label>
          <input
            type="datetime-local"
            value={scheduledAt}
            onChange={(e) => setScheduledAt(e.target.value)}
            className={inputCls}
          />
        </div>
      </div>

      {/* Preview */}
      {(title || body) && (
        <div className="rounded-xl border border-surface-border p-4">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-ink-faint">{dict.broadcastPreview}</p>
          <p className="text-sm font-semibold text-ink">{title || "—"}</p>
          <p className="text-sm text-ink-muted">{body}</p>
        </div>
      )}

      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={preview}
          disabled={pending}
          className="inline-flex items-center gap-1.5 rounded-lg border border-surface-border px-4 py-2 text-sm text-ink hover:bg-surface-hover disabled:opacity-50"
        >
          <Eye className="h-4 w-4" aria-hidden />
          {dict.broadcastPreview}
          {recipients !== null && ` · ${recipients} ${dict.broadcastRecipients}`}
        </button>
        <button
          type="button"
          onClick={send}
          disabled={pending || !title.trim() || !body.trim()}
          className="inline-flex items-center gap-1.5 rounded-lg bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-50"
        >
          <Send className="h-4 w-4" aria-hidden />
          {scheduledAt ? dict.broadcastScheduleBtn : dict.broadcastSendNow}
        </button>
        {result && <span className="text-sm font-medium text-brand-400">{result}</span>}
      </div>
    </div>
  );
}
