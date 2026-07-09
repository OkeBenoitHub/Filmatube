"use client";

import { useEffect, useState } from "react";
import { Bookmark, BookmarkCheck } from "lucide-react";
import { deleteDoc, doc, onSnapshot, serverTimestamp, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Watch Later toggle — writes to watchlists/{uid}/movies/{movieId} (client Firestore). */
export function SaveButton({ movieId, dict }: { movieId: string; dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (!user) return;
    const ref = doc(db, "watchlists", user.uid, "movies", movieId);
    return onSnapshot(ref, (snap) => setSaved(snap.exists()));
  }, [user, movieId]);

  const toggle = async () => {
    if (!user) return;
    const ref = doc(db, "watchlists", user.uid, "movies", movieId);
    if (saved) await deleteDoc(ref);
    else await setDoc(ref, { movieId, addedAt: serverTimestamp() });
  };

  return (
    <button
      type="button"
      onClick={toggle}
      className="inline-flex h-11 items-center gap-2 rounded-lg border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
    >
      {saved ? <BookmarkCheck className="h-4 w-4 text-brand-400" aria-hidden /> : <Bookmark className="h-4 w-4" aria-hidden />}
      {saved ? dict.saved : dict.watchLater}
    </button>
  );
}
