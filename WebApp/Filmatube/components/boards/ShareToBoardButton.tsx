"use client";

import { useState } from "react";
import { Check, MessagesSquare } from "lucide-react";
import {
  addDoc,
  collection,
  getDocs,
  limit,
  query,
  serverTimestamp,
  where,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Modal } from "@/components/ui/Modal";
import { useAuth } from "@/components/providers/AuthProvider";
import { useAuthor } from "@/components/boards/useAuthor";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface BoardOption {
  id: string;
  title: string;
}

/**
 * Share a movie into one of your boards — writes a movie-card message into
 * boards/{id}/messages, mirroring Android `BoardRepository.postMovieCard`.
 */
export function ShareToBoardButton({
  movieId,
  movieTitle,
  moviePoster,
  dict,
}: {
  movieId: string;
  movieTitle: string;
  moviePoster: string;
  dict: Dictionary["catalog"];
}) {
  const { user } = useAuth();
  const author = useAuthor();
  const [open, setOpen] = useState(false);
  const [boards, setBoards] = useState<BoardOption[] | null>(null);
  const [sending, setSending] = useState(false);
  const [sentTo, setSentTo] = useState<string | null>(null);

  if (!user) return null;

  const openPicker = async () => {
    setOpen(true);
    setSentTo(null);
    if (boards) return;
    const snap = await getDocs(
      query(collection(db, "boards"), where("memberIds", "array-contains", user.uid), limit(50)),
    );
    setBoards(snap.docs.map((d) => ({ id: d.id, title: (d.get("title") as string) ?? "" })));
  };

  const share = async (boardId: string) => {
    setSending(true);
    try {
      await addDoc(collection(db, "boards", boardId, "messages"), {
        userId: user.uid,
        userName: author.name,
        userAvatar: author.avatar,
        text: "",
        hasSpoiler: false,
        movieId,
        movieTitle,
        moviePoster,
        createdAt: serverTimestamp(),
      });
      setSentTo(boardId);
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <button
        type="button"
        onClick={openPicker}
        className="inline-flex h-11 items-center whitespace-nowrap gap-2 rounded-lg border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
      >
        <MessagesSquare className="h-4 w-4" aria-hidden />
        {dict.shareToBoard}
      </button>

      <Modal open={open} onClose={() => setOpen(false)}>
        <h2 className="text-lg font-bold text-ink">{dict.shareToBoard}</h2>
        <p className="mt-1 text-sm text-ink-muted">{movieTitle}</p>

        <div className="mt-4 max-h-80 space-y-2 overflow-y-auto">
          {boards === null ? (
            <p className="py-6 text-center text-sm text-ink-muted">{dict.loadingLabel}</p>
          ) : boards.length === 0 ? (
            <p className="py-6 text-center text-sm text-ink-muted">{dict.boardsMineEmpty}</p>
          ) : (
            boards.map((b) => (
              <button
                key={b.id}
                type="button"
                disabled={sending || sentTo === b.id}
                onClick={() => share(b.id)}
                className="flex w-full items-center justify-between gap-2 rounded-lg border border-surface-border px-3 py-2 text-left text-sm text-ink transition-colors hover:bg-surface-hover disabled:opacity-60"
              >
                <span className="truncate font-medium">{b.title}</span>
                {sentTo === b.id && (
                  <span className="inline-flex shrink-0 items-center gap-1 text-xs font-semibold text-brand-400">
                    <Check className="h-3.5 w-3.5" aria-hidden />
                    {dict.sharedToBoard}
                  </span>
                )}
              </button>
            ))
          )}
        </div>

        <button
          type="button"
          onClick={() => setOpen(false)}
          className="mt-4 h-9 w-full rounded-lg border border-surface-border text-sm font-medium text-ink hover:bg-surface-hover"
        >
          {dict.closeLabel}
        </button>
      </Modal>
    </>
  );
}
