"use client";

import { useEffect, useState } from "react";
import { doc, onSnapshot, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface Prefs {
  social: boolean;
  content: boolean;
  system: boolean;
}

const DEFAULTS: Prefs = { social: true, content: true, system: true };

/** Per-channel push opt-in stored at users/{uid}/settings/notifications (a Cloud Function honors it). */
export function NotificationPreferences({ dict }: { dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [prefs, setPrefs] = useState<Prefs>(DEFAULTS);

  useEffect(() => {
    if (!user) return;
    return onSnapshot(doc(db, "users", user.uid, "settings", "notifications"), (snap) => {
      const d = snap.data();
      setPrefs({
        social: d?.social ?? true,
        content: d?.content ?? true,
        system: d?.system ?? true,
      });
    });
  }, [user]);

  const update = async (patch: Partial<Prefs>) => {
    if (!user) return;
    const next = { ...prefs, ...patch };
    setPrefs(next);
    await setDoc(doc(db, "users", user.uid, "settings", "notifications"), next, { merge: true });
  };

  const rows: { key: keyof Prefs; title: string; desc: string }[] = [
    { key: "social", title: dict.notifChannelSocial, desc: dict.notifChannelSocialDesc },
    { key: "content", title: dict.notifChannelContent, desc: dict.notifChannelContentDesc },
    { key: "system", title: dict.notifChannelSystem, desc: dict.notifChannelSystemDesc },
  ];

  return (
    <div className="space-y-4">
      <ul className="divide-y divide-surface-border rounded-xl border border-surface-border">
        {rows.map((r) => (
          <li key={r.key} className="flex items-center justify-between gap-4 px-4 py-3">
            <div>
              <p className="text-sm font-semibold text-ink">{r.title}</p>
              <p className="text-xs text-ink-muted">{r.desc}</p>
            </div>
            <label className="relative inline-flex cursor-pointer items-center">
              <input
                type="checkbox"
                checked={prefs[r.key]}
                onChange={(e) => update({ [r.key]: e.target.checked })}
                className="peer sr-only"
              />
              <div className="h-6 w-11 rounded-full bg-surface-hover after:absolute after:left-0.5 after:top-0.5 after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-all peer-checked:bg-brand-500 peer-checked:after:translate-x-5" />
            </label>
          </li>
        ))}
      </ul>
    </div>
  );
}
