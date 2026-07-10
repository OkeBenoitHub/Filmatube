"use client";

import { useEffect, useState } from "react";
import { UserPlus, UserCheck } from "lucide-react";
import { deleteDoc, doc, onSnapshot, serverTimestamp, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * Follow toggle — writes follows/{followerId}_{followedId} (client Firestore),
 * matching Android FollowRepository. Hidden for your own profile.
 */
export function FollowButton({
  targetUid,
  dict,
  size = "md",
}: {
  targetUid: string;
  dict: Dictionary["catalog"];
  size?: "sm" | "md";
}) {
  const { user } = useAuth();
  const [following, setFollowing] = useState(false);

  const followId = user ? `${user.uid}_${targetUid}` : null;

  useEffect(() => {
    if (!followId) return;
    const ref = doc(db, "follows", followId);
    return onSnapshot(ref, (snap) => setFollowing(snap.exists()));
  }, [followId]);

  if (!user || user.uid === targetUid) return null;

  const toggle = async () => {
    if (!followId) return;
    const ref = doc(db, "follows", followId);
    if (following) {
      await deleteDoc(ref);
    } else {
      await setDoc(ref, {
        followerId: user.uid,
        followedId: targetUid,
        createdAt: serverTimestamp(),
      });
    }
  };

  const base =
    size === "sm"
      ? "h-9 px-4 text-xs"
      : "h-11 px-6 text-sm";

  return (
    <button
      type="button"
      onClick={toggle}
      className={
        following
          ? `inline-flex items-center gap-2 rounded-lg border border-surface-border font-semibold text-ink transition-colors hover:bg-surface-hover ${base}`
          : `inline-flex items-center gap-2 rounded-lg bg-brand-500 font-semibold text-white transition-colors hover:bg-brand-600 ${base}`
      }
    >
      {following ? <UserCheck className="h-4 w-4" aria-hidden /> : <UserPlus className="h-4 w-4" aria-hidden />}
      {following ? dict.followingLabel : dict.follow}
    </button>
  );
}
