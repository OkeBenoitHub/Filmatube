"use client";

import { useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { doc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { GENRE_KEYS } from "@/lib/genres";
import type { Locale } from "@/lib/i18n/config";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { useI18n } from "@/components/providers/LocaleProvider";
import { Button } from "@/components/ui/Button";
import { Chip } from "./Chip";

export function TasteForm({
  uid,
  dict,
  genres,
}: {
  uid: string;
  dict: Dictionary["taste"];
  genres: Dictionary["genres"];
}) {
  const router = useRouter();
  const { locale, setLocale } = useI18n();

  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [appLang, setAppLang] = useState<Locale>(locale);
  const [contentLang, setContentLang] = useState("both");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function toggle(key: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (!next.delete(key)) next.add(key);
      return next;
    });
  }

  async function save() {
    if (selected.size === 0) return;
    setSaving(true);
    setError(null);
    try {
      await updateDoc(doc(db, "users", uid), {
        genrePreferences: [...selected],
        contentLanguage: contentLang,
        language: appLang,
        tasteCompleted: true,
      });
      if (appLang !== locale) setLocale(appLang);
      // Enter the app after onboarding (not the marketing landing page).
      router.push("/home");
      router.refresh();
    } catch {
      setError(dict.saveError);
      setSaving(false);
    }
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
