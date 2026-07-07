"use client";

import { useRef, useState } from "react";
import { Trash2, Upload } from "lucide-react";
import { uploadPublic } from "@/lib/upload/media";
import type { SubtitleTrack } from "@/lib/admin/movie-form";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { Button } from "@/components/ui/Button";

const LANGS: { value: string; label: string }[] = [
  { value: "en", label: "English" },
  { value: "fr", label: "Français" },
];

export function SubtitleTracks({
  tracks,
  onChange,
  dict,
}: {
  tracks: SubtitleTrack[];
  onChange: (tracks: SubtitleTrack[]) => void;
  dict: Dictionary["adminMovies"];
}) {
  const ref = useRef<HTMLInputElement>(null);
  const [lang, setLang] = useState("en");
  const [busy, setBusy] = useState(false);

  async function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setBusy(true);
    try {
      const url = await uploadPublic(file, "subtitles");
      onChange([...tracks.filter((t) => t.lang !== lang), { lang, url }]);
    } catch {
      /* ignore */
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-ink">{dict.subtitles}</label>

      {tracks.length > 0 && (
        <ul className="space-y-1">
          {tracks.map((t) => (
            <li key={t.lang} className="flex items-center gap-2 text-sm text-ink">
              <span className="rounded bg-surface-hover px-2 py-0.5 text-xs uppercase">{t.lang}</span>
              <span className="flex-1 truncate text-ink-muted">{t.url}</span>
              <button
                type="button"
                onClick={() => onChange(tracks.filter((x) => x.lang !== t.lang))}
                className="text-ink-faint hover:text-red-300"
                aria-label={dict.remove}
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="flex items-center gap-2">
        <select
          value={lang}
          onChange={(e) => setLang(e.target.value)}
          className="h-9 rounded-lg border border-surface-border bg-surface px-2 text-sm text-ink"
        >
          {LANGS.map((l) => (
            <option key={l.value} value={l.value}>{l.label}</option>
          ))}
        </select>
        <Button type="button" variant="outline" size="sm" onClick={() => ref.current?.click()} loading={busy}>
          <Upload className="mr-1.5 h-3.5 w-3.5" aria-hidden />
          {dict.addSubtitle}
        </Button>
        <input ref={ref} type="file" accept=".vtt,text/vtt" hidden onChange={onPick} />
      </div>
    </div>
  );
}
