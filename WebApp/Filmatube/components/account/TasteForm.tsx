"use client";

import { useState, useTransition, type ReactNode } from "react";
import { saveTaste } from "@/app/account/taste/actions";
import { GENRE_KEYS } from "@/lib/genres";
import type { Locale } from "@/lib/i18n/config";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { useI18n } from "@/components/providers/LocaleProvider";
import { Button } from "@/components/ui/Button";
import { Chip } from "./Chip";

export function TasteForm({ dict, genres }: { dict: Dictionary["taste"]; genres: Dictionary["genres"] }) {
  // The uid is no longer passed in — the action reads it from the session cookie instead.
  const { locale } = useI18n();

  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [appLang, setAppLang] = useState<Locale>(locale);
  const [contentLang, setContentLang] = useState("both");
  const [saving, startSaving] = useTransition();
  const [error, setError] = useState<string | null>(null);

  function toggle(key: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (!next.delete(key)) next.add(key);
      return next;
    });
  }

  function save() {
    if (selected.size === 0) return;
    setError(null);
    startSaving(async () => {
      try {
        // Writes the prefs, sets the locale cookie and redirects to /home server-side —
        // the button's spinner is tied to this transition, so it always resolves.
        const result = await saveTaste({
          genres: [...selected],
          contentLanguage: contentLang,
          appLanguage: appLang,
        });
        if (result?.error) setError(dict.saveError);
      } catch (e) {
        // redirect() throws NEXT_REDIRECT — that's the success path, not a failure.
        if (typeof e === "object" && e !== null && "digest" in e) throw e;
        setError(dict.saveError);
      }
    });
  }

  return (
    <div className="mx-auto max-w-xl space-y-6 px-6 py-10">
      <div>
        <h1 className="text-2xl font-bold text-ink">{dict.title}</h1>
        <p className="mt-1 text-sm text-ink-muted">{dict.subtitle}</p>
      </div>

      <Section title={dict.appLanguage}>
        <Chip label="English" selected={appLang === "en"} onClick={() => setAppLang("en")} />
        <Chip label="Français" selected={appLang === "fr"} onClick={() => setAppLang("fr")} />
      </Section>

      <Section title={dict.contentLanguage}>
        <Chip label={dict.contentEn} selected={contentLang === "en"} onClick={() => setContentLang("en")} />
        <Chip label={dict.contentFr} selected={contentLang === "fr"} onClick={() => setContentLang("fr")} />
        <Chip label={dict.contentBoth} selected={contentLang === "both"} onClick={() => setContentLang("both")} />
      </Section>

      <Section title={dict.genres} hint={dict.hint}>
        {GENRE_KEYS.map((key) => (
          <Chip key={key} label={genres[key]} selected={selected.has(key)} onClick={() => toggle(key)} />
        ))}
      </Section>

      {error && (
        <p className="rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-400">{error}</p>
      )}
      <Button onClick={save} loading={saving} disabled={selected.size === 0} className="w-full">
        {dict.continue}
      </Button>
    </div>
  );
}

function Section({ title, hint, children }: { title: string; hint?: string; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <h2 className="font-semibold text-ink">{title}</h2>
      {hint && <p className="text-xs text-ink-muted">{hint}</p>}
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
