"use client";

import { useState } from "react";
import { Check, UserPlus } from "lucide-react";
import {
  addDoc,
  collection,
  getDocs,
  query,
  serverTimestamp,
  where,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { useAuthor } from "@/components/boards/useAuthor";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * Invite your followers to a board — writes a `board_invite` notification into each inbox,
 * mirroring Android `BoardRepository.inviteFollowers`.
 */
export function InviteFollowersButton({
  boardId,
  boardTitle,
  dict,
}: {
  boardId: string;
  boardTitle: string;
  dict: Dictionary["catalog"];
}) {
  const { user } = useAuth();
  const author = useAuthor();
  const [busy, setBusy] = useState(false);
  const [invited, setInvited] = useState<number | null>(null);

  if (!user) return null;

  const invite = async () => {
    setBusy(true);
    try {
      const snap = await getDocs(query(collection(db, "follows"), where("followedId", "==", user.uid)));
      const followerIds = snap.docs.map((d) => d.get("followerId") as string).filter(Boolean);

      const results = await Promise.all(
        followerIds.map((followerId) =>
          addDoc(collection(db, "users", followerId, "notifications"), {
            type: "board_invite",
            actorId: user.uid,
            actorName: author.name,
            actorAvatar: author.avatar,
            boardId,
            boardTitle,
            read: false,
            createdAt: serverTimestamp(),
          })
            .then(() => true)
            .catch(() => false),
        ),
      );
      setInvited(results.filter(Boolean).length);
    } finally {
      setBusy(false);
    }
  };

  return (
    <button
      type="button"
      onClick={invite}
      disabled={busy || invited !== null}
      className="inline-flex h-9 items-center gap-1.5 rounded-full border border-surface-border px-4 text-sm font-medium text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink disabled:opacity-60"
    >
      {invited !== null ? <Check className="h-4 w-4" aria-hidden /> : <UserPlus className="h-4 w-4" aria-hidden />}
      {invited !== null ? `${dict.invitedCount} ${invited}` : dict.inviteFollowers}
    </button>
  );
}
