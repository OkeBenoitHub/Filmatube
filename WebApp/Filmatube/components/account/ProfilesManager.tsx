"use client";

import { useEffect, useRef, useState } from "react";
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  serverTimestamp,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";

const EMOJIS = ["🍿", "🎬", "🎥", "😎", "🐱", "🦄", "🚀", "🎮", "🌟", "🐸", "👾", "🎧"];
const ACTIVE_KEY = "filmatube_active_profile";

interface Profile {
  id: string;
  name: string;
  avatarEmoji: string;
  isDefault: boolean;
}

export function ProfilesManager({
  uid,
  defaultName,
  dict,
}: {
  uid: string;
  defaultName: string;
  dict: Dictionary["account"];
}) {
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [active, setActive] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [name, setName] = useState("");
  const [emoji, setEmoji] = useState(EMOJIS[0]);
  const ensuredDefault = useRef(false);

  useEffect(() => {
    setActive(localStorage.getItem(ACTIVE_KEY));
  }, []);

  useEffect(() => {
    const col = collection(db, "users", uid, "profiles");
    const unsub = onSnapshot(col, (snap) => {
      const list: Profile[] = snap.docs.map((d) => ({
        id: d.id,
        name: d.get("name") ?? "",
        avatarEmoji: d.get("avatarEmoji") ?? "🍿",
        isDefault: d.get("isDefault") ?? false,
      }));
      if (list.length === 0 && !ensuredDefault.current) {
        ensuredDefault.current = true;
        void addDoc(col, {
          name: defaultName,
          avatarEmoji: "🍿",
          isDefault: true,
          language: "en",
          createdAt: serverTimestamp(),
        });
        return;
      }
      list.sort((a, b) => Number(b.isDefault) - Number(a.isDefault));
      setProfiles(list);
    });
    return () => unsub();
  }, [uid, defaultName]);

  function switchTo(id: string) {
    localStorage.setItem(ACTIVE_KEY, id);
    setActive(id);
  }

  async function create() {
    if (!name.trim()) return;
    await addDoc(collection(db, "users", uid, "profiles"), {
      name: name.trim(),
      avatarEmoji: emoji,
      isDefault: false,
      language: "en",
      createdAt: serverTimestamp(),
    });
    setName("");
    setEmoji(EMOJIS[0]);
    setAdding(false);
  }

  async function remove(id: string) {
    await deleteDoc(doc(db, "users", uid, "profiles", id));
  }

  return (
    <div className="mx-auto max-w-xl px-6 py-10">
      <h1 className="text-2xl font-bold text-ink">{dict.profiles}</h1>

      <div className="mt-6 grid grid-cols-3 gap-4 sm:grid-cols-4">
        {profiles.map((p) => (
          <div key={p.id} className="flex flex-col items-center gap-1.5">
            <button
              type="button"
              onClick={() => switchTo(p.id)}
              className={cn(
                "flex h-16 w-16 items-center justify-center rounded-full bg-surface-hover text-2xl transition-shadow",
                active === p.id && "ring-2 ring-brand-500",
              )}
            >
              {p.avatarEmoji}
            </button>
            <span className="text-sm text-ink">{p.name}</span>
            {active === p.id && <span className="text-xs text-brand-400">{dict.active}</span>}
            {!p.isDefault && (
              <button
                type="button"
                onClick={() => remove(p.id)}
                className="text-xs text-ink-faint hover:text-red-300"
              >
                {dict.delete}
              </button>
            )}
          </div>
        ))}

        <button
          type="button"
          onClick={() => setAdding(true)}
          className="flex flex-col items-center gap-1.5"
        >
          <span className="flex h-16 w-16 items-center justify-center rounded-full border border-dashed border-surface-border text-2xl text-ink-muted">
            +
          </span>
          <span className="text-sm text-ink-muted">{dict.addProfile}</span>
        </button>
      </div>

      {adding && (
        <div className="mt-8 space-y-3 rounded-xl border border-surface-border bg-surface-card p-4">
          <Input placeholder={dict.profileName} value={name} onChange={(e) => setName(e.target.value)} />
          <div className="flex flex-wrap gap-2">
            {EMOJIS.map((e) => (
              <button
                key={e}
                type="button"
                onClick={() => setEmoji(e)}
                className={cn(
                  "flex h-10 w-10 items-center justify-center rounded-full bg-surface-hover text-lg",
                  emoji === e && "ring-2 ring-brand-500",
                )}
              >
                {e}
              </button>
            ))}
          </div>
          <div className="flex gap-2">
            <Button onClick={create} disabled={!name.trim()} className="flex-1">
              {dict.create}
            </Button>
            <Button variant="outline" onClick={() => setAdding(false)} className="flex-1">
              {dict.cancel}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
