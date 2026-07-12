"use client";

import { useEffect, useState } from "react";
import { Send } from "lucide-react";
import { addDoc, collection, onSnapshot, orderBy, query, serverTimestamp, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface RequestRow {
  id: string;
  title: string;
  note: string;
  status: string;
  reason: string;
}

/** Content request form + the user's own requests with status. Writes requests/{id}. */
export function RequestForm({ dict }: { dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [title, setTitle] = useState("");
  const [note, setNote] = useState("");
  const [done, setDone] = useState(false);
  const [mine, setMine] = useState<RequestRow[]>([]);

  useEffect(() => {
    if (!user) return;
    const q = query(collection(db, "requests"), where("userId", "==", user.uid), orderBy("createdAt", "desc"));
    return onSnapshot(q, (snap) => {
      setMine(
        snap.docs.map((d) => ({
          id: d.id,
          title: d.get("title") ?? "",
          note: d.get("note") ?? "",
          status: d.get("status") ?? "pending",
          reason: d.get("reason") ?? "",
        })),
      );
    });
  }, [user]);

  const submit = async () => {
    if (!user || !title.trim()) return;
    await addDoc(collection(db, "requests"), {
      userId: user.uid,
      userName: user.displayName ?? "",
      title: title.trim(),
      note: note.trim(),
      status: "pending",
      reason: "",
      createdAt: serverTimestamp(),
    });
    setTitle("");
    setNote("");
    setDone(true);
  };

  const statusLabel = (s: string) =>
    s === "approved" ? dict.statusApproved : s === "rejected" ? dict.statusRejected : dict.statusPending;

  const inputCls =
    "w-full rounded-lg border border-surface-border bg-surface-hover px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-brand-500 focus:outline-none";

  return (
    <div className="space-y-8">
      <div className="space-y-3">
        <input
          value={title}
          onChange={(e) => {
            setTitle(e.target.value);
            setDone(false);
          }}
          placeholder={dict.requestFieldTitle}
          className={inputCls}
        />
        <textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder={dict.requestFieldNote}
          rows={3}
          className={`${inputCls} resize-none`}
        />
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={submit}
            disabled={!title.trim()}
            className="inline-flex items-center gap-1.5 rounded-lg bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-50"
          >
            <Send className="h-4 w-4" aria-hidden />
            {dict.requestSubmit}
          </button>
          {done && <span className="text-sm font-medium text-brand-400">{dict.requestSubmitted}</span>}
        </div>
      </div>

      <div className="space-y-3">
        <h2 className="text-lg font-semibold text-ink">{dict.yourRequests}</h2>
        {mine.length === 0 ? (
          <p className="text-sm text-ink-muted">{dict.noRequests}</p>
        ) : (
          <ul className="divide-y divide-surface-border rounded-xl border border-surface-border">
            {mine.map((r) => (
              <li key={r.id} className="flex items-center justify-between gap-3 px-4 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-ink">{r.title}</p>
                  {r.reason && <p className="truncate text-xs text-ink-muted">{r.reason}</p>}
                </div>
                <span
                  className={
                    r.status === "approved"
                      ? "shrink-0 rounded-full bg-brand-500/15 px-2.5 py-1 text-xs font-semibold text-brand-400"
                      : r.status === "rejected"
                        ? "shrink-0 rounded-full bg-red-500/15 px-2.5 py-1 text-xs font-semibold text-red-400"
                        : "shrink-0 rounded-full bg-surface-hover px-2.5 py-1 text-xs text-ink-muted"
                  }
                >
                  {statusLabel(r.status)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
