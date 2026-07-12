"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Bell } from "lucide-react";
import { collection, onSnapshot, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Header bell linking to /notifications with a live unread-count badge. */
export function NotificationBell({ dict }: { dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (!user) return;
    const q = query(collection(db, "users", user.uid, "notifications"), where("read", "==", false));
    return onSnapshot(q, (snap) => setUnread(snap.size));
  }, [user]);

  return (
    <Link
      href="/notifications"
      aria-label={dict.notificationsTitle}
      className="relative flex h-9 w-9 items-center justify-center rounded-lg border border-surface-border text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink"
    >
      <Bell className="h-4 w-4" aria-hidden />
      {unread > 0 && (
        <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-brand-500 px-1 text-[10px] font-bold text-white">
          {unread > 9 ? "9+" : unread}
        </span>
      )}
    </Link>
  );
}
