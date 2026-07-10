"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ThumbsUp, Trash2 } from "lucide-react";
import {
  collection,
  deleteDoc,
  doc,
  getDocs,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { UserAvatar } from "@/components/social/UserList";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface ReviewItem {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  text: string;
  hasSpoiler: boolean;
  likeCount: number;
  likedByMe: boolean;
  isMine: boolean;
}

/** Public reviews for a movie — mirrors Android ReviewRepository (reviews/{movieId}/items/{uid}). */
export function ReviewsSection({ movieId, dict }: { movieId: string; dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [reviews, setReviews] = useState<ReviewItem[]>([]);
  const [text, setText] = useState("");
  const [spoiler, setSpoiler] = useState(false);
  const [revealed, setRevealed] = useState<Record<string, boolean>>({});

  useEffect(() => {
    const q = query(collection(db, "reviews", movieId, "items"), orderBy("createdAt", "desc"));
    return onSnapshot(q, async (snap) => {
      const items = await Promise.all(
        snap.docs.map(async (d) => {
          const likes = await getDocs(collection(db, "reviews", movieId, "items", d.id, "likes"));
          return {
            id: d.id,
            userId: d.get("userId") ?? "",
            userName: d.get("userName") ?? "",
            userAvatar: d.get("userAvatar") ?? "",
            text: d.get("text") ?? "",
            hasSpoiler: d.get("hasSpoiler") ?? false,
            likeCount: likes.size,
            likedByMe: !!user && likes.docs.some((l) => l.id === user.uid),
            isMine: !!user && d.id === user.uid,
          } as ReviewItem;
        }),
      );
      setReviews(items);
    });
  }, [movieId, user]);

  const mine = reviews.find((r) => r.isMine);

  useEffect(() => {
    if (mine) {
      setText(mine.text);
      setSpoiler(mine.hasSpoiler);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mine?.id]);

  const submit = async () => {
    if (!user || !text.trim()) return;
    await setDoc(doc(db, "reviews", movieId, "items", user.uid), {
      userId: user.uid,
      userName: user.displayName ?? "",
      userAvatar: user.photoURL ?? "",
      text: text.trim(),
      hasSpoiler: spoiler,
      createdAt: serverTimestamp(),
    });
  };

  const remove = async () => {
    if (!user) return;
    await deleteDoc(doc(db, "reviews", movieId, "items", user.uid));
    setText("");
    setSpoiler(false);
  };

  const toggleLike = async (review: ReviewItem) => {
    if (!user) return;
    const ref = doc(db, "reviews", movieId, "items", review.id, "likes", user.uid);
    if (review.likedByMe) await deleteDoc(ref);
    else await setDoc(ref, { userId: user.uid, createdAt: serverTimestamp() });
  };

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold text-ink">{dict.reviewsHeading}</h2>

      {user && (
        <div className="space-y-2 rounded-xl border border-surface-border p-4">
          <p className="text-sm font-semibold text-ink">{mine ? dict.yourReviewLabel : dict.writeReview}</p>
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder={dict.reviewPlaceholder}
            rows={3}
            className="w-full resize-none rounded-lg border border-surface-border bg-surface-hover px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-brand-500 focus:outline-none"
          />
          <div className="flex flex-wrap items-center justify-between gap-2">
            <label className="flex items-center gap-2 text-sm text-ink-muted">
              <input type="checkbox" checked={spoiler} onChange={(e) => setSpoiler(e.target.checked)} />
              {dict.spoilerToggle}
            </label>
            <div className="flex items-center gap-2">
              {mine && (
                <button
                  type="button"
                  onClick={remove}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-surface-border px-3 py-1.5 text-sm text-ink-muted hover:bg-surface-hover"
                >
                  <Trash2 className="h-4 w-4" aria-hidden />
                  {dict.deleteReview}
                </button>
              )}
              <button
                type="button"
                onClick={submit}
                disabled={!text.trim()}
                className="rounded-lg bg-brand-500 px-4 py-1.5 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-50"
              >
                {mine ? dict.updateReview : dict.postReview}
              </button>
            </div>
          </div>
        </div>
      )}

      {reviews.length === 0 ? (
        <p className="py-4 text-sm text-ink-muted">{dict.reviewsEmpty}</p>
      ) : (
        <ul className="space-y-4">
          {reviews.map((r) => (
            <li key={r.id} className="space-y-2 border-b border-surface-border pb-4 last:border-0">
              <div className="flex items-center gap-2">
                <Link href={`/u/${r.userId}`}>
                  <UserAvatar name={r.userName} url={r.userAvatar} size={32} />
                </Link>
                <Link href={`/u/${r.userId}`} className="text-sm font-semibold text-ink hover:underline">
                  {r.userName}
                </Link>
                {r.isMine && <span className="text-xs text-brand-400">{dict.youLabel}</span>}
              </div>

              {r.hasSpoiler && !revealed[r.id] ? (
                <button
                  type="button"
                  onClick={() => setRevealed((prev) => ({ ...prev, [r.id]: true }))}
                  className="w-full rounded-lg bg-surface-hover px-3 py-3 text-left text-sm text-ink-muted"
                >
                  {dict.spoilerHidden}
                </button>
              ) : (
                <>
                  {r.hasSpoiler && <span className="text-xs font-semibold text-red-400">{dict.spoilerChip}</span>}
                  <p className="text-sm text-ink">{r.text}</p>
                </>
              )}

              <button
                type="button"
                onClick={() => toggleLike(r)}
                disabled={!user}
                className={
                  r.likedByMe
                    ? "inline-flex items-center gap-1.5 text-sm font-semibold text-brand-400"
                    : "inline-flex items-center gap-1.5 text-sm text-ink-muted hover:text-ink disabled:opacity-50"
                }
              >
                <ThumbsUp className="h-4 w-4" aria-hidden />
                {r.likeCount > 0 ? r.likeCount : dict.likeAction}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
