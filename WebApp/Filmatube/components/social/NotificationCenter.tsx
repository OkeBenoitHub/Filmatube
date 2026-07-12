"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { collection, doc, limit, onSnapshot, orderBy, query, updateDoc, writeBatch, Timestamp } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { UserAvatar } from "@/components/social/UserList";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface Notification {
  id: string;
  type: string;
  actorId: string;
  actorName: string;
  actorAvatar: string;
  movieId: string;
  movieTitle: string;
  read: boolean;
  createdAtMs: number;
}

function actionText(type: string, dict: Dictionary["catalog"]): string {
  switch (type) {
    case "follow":
      return dict.notifFollow;
    case "recommendation":
      return dict.notifRecommendation;
    case "reply":
      return dict.notifReply;
    case "review_like":
      return dict.notifReviewLike;
    default:
      return dict.notifRecommendation;
  }
}

/** In-app notification center reading users/{uid}/notifications (realtime). Mirrors Android. */
export function NotificationCenter({ dict }: { dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const router = useRouter();
  const [items, setItems] = useState<Notification[]>([]);

  useEffect(() => {
    if (!user) return;
    const q = query(
      collection(db, "users", user.uid, "notifications"),
      orderBy("createdAt", "desc"),
      limit(50),
    );
    return onSnapshot(q, (snap) => {
      setItems(
        snap.docs.map((d) => {
          const ts = d.get("createdAt") as Timestamp | undefined;
          return {
            id: d.id,
            type: d.get("type") ?? "",
            actorId: d.get("actorId") ?? "",
            actorName: d.get("actorName") ?? "",
            actorAvatar: d.get("actorAvatar") ?? "",
            movieId: d.get("movieId") ?? "",
            movieTitle: d.get("movieTitle") ?? "",
            read: d.get("read") ?? false,
            createdAtMs: ts ? ts.toMillis() : 0,
          };
        }),
      );
    });
  }, [user]);

  const markAllRead = async () => {
    if (!user) return;
    const batch = writeBatch(db);
    items.filter((n) => !n.read).forEach((n) => batch.update(doc(db, "users", user.uid, "notifications", n.id), { read: true }));
    await batch.commit();
  };

  const open = async (n: Notification) => {
    if (user && !n.read) await updateDoc(doc(db, "users", user.uid, "notifications", n.id), { read: true });
    if (n.movieId) router.push(`/movie/${n.movieId}`);
    else if (n.actorId) router.push(`/u/${n.actorId}`);
  };

  const dayAgo = Date.now() - 24 * 3600e3;
  const today = items.filter((n) => n.createdAtMs >= dayAgo);
  const earlier = items.filter((n) => n.createdAtMs < dayAgo);

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 md:px-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-ink">{dict.notificationsTitle}</h1>
        {items.some((n) => !n.read) && (
          <button type="button" onClick={markAllRead} className="text-sm text-brand-400 hover:underline">
            {dict.markAllRead}
          </button>
        )}
      </div>

      {items.length === 0 ? (
        <p className="py-12 text-center text-ink-muted">{dict.notificationsEmpty}</p>
      ) : (
        <div className="mt-6 space-y-6">
          {today.length > 0 && <Group title={dict.filterToday} items={today} dict={dict} onOpen={open} />}
          {earlier.length > 0 && <Group title={dict.notifEarlier} items={earlier} dict={dict} onOpen={open} />}
        </div>
      )}
    </div>
  );
}

function Group({
  title,
  items,
  dict,
  onOpen,
}: {
  title: string;
  items: Notification[];
  dict: Dictionary["catalog"];
  onOpen: (n: Notification) => void;
}) {
  return (
    <div>
      <h2 className="mb-2 text-xs font-semibold uppercase tracking-wide text-ink-faint">{title}</h2>
      <ul className="divide-y divide-surface-border">
        {items.map((n) => (
          <li key={n.id}>
            <button
              type="button"
              onClick={() => onOpen(n)}
              className={`flex w-full items-center gap-3 px-2 py-3 text-left ${n.read ? "" : "bg-surface-hover/50"}`}
            >
              <UserAvatar name={n.actorName} url={n.actorAvatar} size={40} />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm text-ink">
                  <span className="font-semibold">{n.actorName}</span> {actionText(n.type, dict)}
                </p>
                {n.movieTitle && <p className="truncate text-xs text-ink-muted">{n.movieTitle}</p>}
              </div>
              {!n.read && <span className="h-2.5 w-2.5 shrink-0 rounded-full bg-brand-500" aria-hidden />}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
