"use client";

import { useState } from "react";
import { Trash2, Plus } from "lucide-react";
import type { AudioTrackEntry } from "@/lib/admin/movie-form";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";

const LANGS = [
  { value: "en", label: "English" },
  { value: "fr", label: "Français" },
];

/** Declares the audio languages available in the movie's video (label is informational). */
export function AudioTracks({
  tracks,
  onChange,
  dict,
}: {
  tracks: AudioTrackEntry[];
  onChange: (tracks: AudioTrackEntry[]) => void;
  dict: Dictionary["adminMovies"];
}) {
  const [lang, setLang] = useState("en");
  const [label, setLabel] = useState("");

  const add = () => {
    const clean = label.trim();
    if (!clean) return;
    onChange([...tracks.filter((t) => t.lang !== lang), { lang, label: clean }]);
    setLabel("");
  };

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-ink">{dict.audioTracks}</label>

      {tracks.length > 0 && (
        <ul className="space-y-1.5">
          {tracks.map((t) => (
            <li
              key={t.lang}
              className="flex items-center justify-between rounded-lg border border-surface-border px-3 py-2 text-sm"
            >
              <span className="text-ink">
                <span className="text-ink-muted">{t.lang.toUpperCase()}</span> — {t.label}
              </span>
              <button
                type="button"
                onClick={() => onChange(tracks.filter((x) => x.lang !== t.lang))}
                aria-label={dict.remove}
                className="text-red-300 hover:text-red-200"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="flex gap-2">
        <select
          value={lang}
          onChange={(e) => setLang(e.target.value)}
          className="h-10 rounded-lg border border-surface-border bg-surface px-3 text-sm text-ink"
        >
          {LANGS.map((l) => (
            <option key={l.value} value={l.value}>
              {l.label}
            </option>
          ))}
        </select>
        <Input placeholder={dict.audioLabel} value={label} onChange={(e) => setLabel(e.target.value)} />
        <Button type="button" variant="secondary" onClick={add}>
          <Plus className="h-4 w-4" aria-hidden />
          {dict.addAudio}
        </Button>
      </div>
    </div>
  );
}
