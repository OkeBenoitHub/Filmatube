"use client";

import { useState } from "react";
import { Send, X } from "lucide-react";
import {
  addDoc,
  collection,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  where,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { UserAvatar } from "@/components/social/UserList";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface Recipient {
  uid: string;
  displayName: string;
  avatarUrl: string;
}

/**
 * Recommend a movie to someone you follow — writes recommendations/{toUid}/items,
 * mirroring Android RecommendationRepository.send.
 */
export function RecommendButton({
  movieId,
  movieTitle,
  dict,
}: {
  movieId: string;
  movieTitle: string;
  dict: Dictionary["catalog"];
}) {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [recipients, setRecipients] = useState<Recipient[] | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  const loadRecipients = async () => {
    if (!user) return;
    const snap = await getDocs(query(collection(db, "follows"), where("followerId", "==", user.uid)));
    const ids = snap.docs.map((d) => d.get("followedId") as string).filter(Boolean);
    const users = await Promise.all(
      ids.map(async (id) => {
        const u = await getDoc(doc(db, "users", id));
        if (!u.exists()) return null;
        return { uid: id, displayName: u.get("displayName") ?? "", avatarUrl: u.get("avatarUrl") ?? "" } as Recipient;
      }),
    );
    setRecipients(users.filter((u): u is Recipient => u !== null));
  };

  const openDialog = () => {
    setOpen(true);
    setSent(false);
    setSelected(null);
    setMessage("");
    if (recipients === null) loadRecipients();
  };

  const send = async () => {
    if (!user || !selected) return;
    setSending(true);
    try {
      const me = await getDoc(doc(db, "users", user.uid));
      await addDoc(collection(db, "recommendations", selected, "items"), {
        fromUserId: user.uid,
        fromName: me.get("displayName") ?? "",
        fromAvatar: me.get("avatarUrl") ?? "",
        movieId,
        movieTitle,
        message: message.trim(),
        createdAt: serverTimestamp(),
      });
      setSent(true);
    } finally {
      setSending(false);
    }
  };

  if (!user) return null;

  return (
    <>
      <button
        type="button"
        onClick={openDialog}
        className="inline-flex h-11 items-center gap-2 rounded-lg border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
      >
        <Send className="h-4 w-4" aria-hidden />
        {dict.recommend}
      </button>

      {open && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 p-0 sm:items-center sm:p-4"
          onClick={() => setOpen(false)}
        >
          <div
            className="w-full max-w-md rounded-t-2xl border border-surface-border bg-surface p-5 sm:rounded-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-ink">{dict.recommendTitle}</h2>
              <button type="button" onClick={() => setOpen(false)} aria-label={dict.goBack} className="text-ink-muted hover:text-ink">
                <X className="h-5 w-5" aria-hidden />
              </button>
            </div>
            <p className="mt-1 truncate text-sm text-ink-muted">{movieTitle}</p>

            {sent ? (
              <p className="py-8 text-center text-sm font-semibold text-brand-400">{dict.recommendSent}</p>
            ) : recipients === null ? (
              <p className="py-8 text-center text-sm text-ink-muted">…</p>
            ) : recipients.length === 0 ? (
              <p className="py-8 text-center text-sm text-ink-muted">{dict.recommendNoFollowing}</p>
            ) : (
              <>
                <ul className="mt-4 max-h-56 space-y-1 overflow-y-auto">
                  {recipients.map((r) => (
                    <li key={r.uid}>
                      <button
                        type="button"
                        onClick={() => setSelected(r.uid)}
                        className={
                          selected === r.uid
                            ? "flex w-full items-center gap-3 rounded-lg border border-brand-500 bg-brand-500/10 px-3 py-2 text-left"
                            : "flex w-full items-center gap-3 rounded-lg border border-transparent px-3 py-2 text-left hover:bg-surface-hover"
                        }
                      >
                        <UserAvatar name={r.displayName} url={r.avatarUrl} size={36} />
                        <span className="truncate text-sm font-medium text-ink">{r.displayName}</span>
                      </button>
                    </li>
                  ))}
                </ul>
                <textarea
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder={dict.recommendMessage}
                  rows={2}
                  className="mt-3 w-full resize-none rounded-lg border border-surface-border bg-surface-hover px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-brand-500 focus:outline-none"
                />
                <button
                  type="button"
                  onClick={send}
                  disabled={!selected || sending}
                  className="mt-3 inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-brand-500 text-sm font-semibold text-white transition-colors hover:bg-brand-600 disabled:opacity-50"
                >
                  <Send className="h-4 w-4" aria-hidden />
                  {dict.recommendSend}
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
}
