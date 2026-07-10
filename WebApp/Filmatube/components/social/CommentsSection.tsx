"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ThumbsUp, Trash2, Send, Flag } from "lucide-react";
import {
  addDoc,
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

interface CommentItem {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  text: string;
  hasSpoiler: boolean;
  parentId: string | null;
  likeCount: number;
  likedByMe: boolean;
  isMine: boolean;
}

/** Threaded comments — comments/{movieId}/items with parentId (1-level replies). Mirrors Android. */
export function CommentsSection({ movieId, dict }: { movieId: string; dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [text, setText] = useState("");
  const [spoiler, setSpoiler] = useState(false);
  const [replyTo, setReplyTo] = useState<CommentItem | null>(null);
  const [revealed, setRevealed] = useState<Record<string, boolean>>({});
  const [reported, setReported] = useState<Record<string, boolean>>({});

  useEffect(() => {
    const q = query(collection(db, "comments", movieId, "items"), orderBy("createdAt", "asc"));
    return onSnapshot(q, async (snap) => {
      const items = await Promise.all(
        snap.docs.map(async (d) => {
          const likes = await getDocs(collection(db, "comments", movieId, "items", d.id, "likes"));
          return {
            id: d.id,
            userId: d.get("userId") ?? "",
            userName: d.get("userName") ?? "",
            userAvatar: d.get("userAvatar") ?? "",
            text: d.get("text") ?? "",
            hasSpoiler: d.get("hasSpoiler") ?? false,
            parentId: d.get("parentId") ?? null,
            likeCount: likes.size,
            likedByMe: !!user && likes.docs.some((l) => l.id === user.uid),
            isMine: !!user && d.get("userId") === user.uid,
          } as CommentItem;
        }),
      );
      setComments(items);
    });
  }, [movieId, user]);

  const post = async () => {
    if (!user || !text.trim()) return;
    await addDoc(collection(db, "comments", movieId, "items"), {
      userId: user.uid,
      userName: user.displayName ?? "",
      userAvatar: user.photoURL ?? "",
      text: text.trim(),
      hasSpoiler: spoiler,
      parentId: replyTo?.id ?? null,
      createdAt: serverTimestamp(),
    });
    setText("");
    setSpoiler(false);
    setReplyTo(null);
  };

  const remove = async (c: CommentItem) => {
    await deleteDoc(doc(db, "comments", movieId, "items", c.id));
  };

  const toggleLike = async (c: CommentItem) => {
    if (!user) return;
    const ref = doc(db, "comments", movieId, "items", c.id, "likes", user.uid);
    if (c.likedByMe) await deleteDoc(ref);
    else await setDoc(ref, { userId: user.uid, createdAt: serverTimestamp() });
  };

  const report = async (c: CommentItem) => {
    if (!user) return;
    await addDoc(collection(db, "reports"), {
      type: "comment",
      movieId,
      targetId: c.id,
      reportedUserId: c.userId,
      reporterId: user.uid,
      reason: "",
      status: "pending",
      createdAt: serverTimestamp(),
    });
    setReported((prev) => ({ ...prev, [c.id]: true }));
  };

  const topLevel = comments.filter((c) => c.parentId === null);

  const renderComment = (c: CommentItem, isReply: boolean) => (
    <div key={c.id} className={isReply ? "ml-10 space-y-1.5" : "space-y-1.5"}>
      <div className="flex items-center gap-2">
        <Link href={`/u/${c.userId}`}>
          <UserAvatar name={c.userName} url={c.userAvatar} size={28} />
        </Link>
        <Link href={`/u/${c.userId}`} className="text-sm font-semibold text-ink hover:underline">
          {c.userName}
        </Link>
        {c.isMine && <span className="text-xs text-brand-400">{dict.youLabel}</span>}
      </div>

      {c.hasSpoiler && !revealed[c.id] ? (
        <button
          type="button"
          onClick={() => setRevealed((prev) => ({ ...prev, [c.id]: true }))}
          className="w-full rounded-lg bg-surface-hover px-3 py-2 text-left text-sm text-ink-muted"
        >
          {dict.spoilerHidden}
        </button>
      ) : (
        <>
          {c.hasSpoiler && <span className="text-xs font-semibold text-red-400">{dict.spoilerChip}</span>}
          <p className="text-sm text-ink">{c.text}</p>
        </>
      )}

      <div className="flex items-center gap-3 text-sm text-ink-muted">
        <button
          type="button"
          onClick={() => toggleLike(c)}
          disabled={!user}
          className={c.likedByMe ? "inline-flex items-center gap-1 font-semibold text-brand-400" : "inline-flex items-center gap-1 hover:text-ink disabled:opacity-50"}
        >
          <ThumbsUp className="h-3.5 w-3.5" aria-hidden />
          {c.likeCount > 0 ? c.likeCount : dict.likeAction}
        </button>
        {!isReply && user && (
          <button type="button" onClick={() => setReplyTo(c)} className="hover:text-ink">
            {dict.replyAction}
          </button>
        )}
        {c.isMine ? (
          <button type="button" onClick={() => remove(c)} className="inline-flex items-center gap-1 hover:text-ink">
            <Trash2 className="h-3.5 w-3.5" aria-hidden />
            {dict.deleteReview}
          </button>
        ) : (
          user && (
            <button
              type="button"
              onClick={() => report(c)}
              disabled={reported[c.id]}
              className="inline-flex items-center gap-1 hover:text-ink disabled:opacity-50"
            >
              <Flag className="h-3.5 w-3.5" aria-hidden />
              {reported[c.id] ? dict.reportedAction : dict.reportAction}
            </button>
          )
        )}
      </div>
    </div>
  );

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold text-ink">{dict.commentsHeading}</h2>

      {user && (
        <div className="space-y-2 rounded-xl border border-surface-border p-4">
          {replyTo && (
            <div className="flex items-center justify-between text-sm text-brand-400">
              <span>
                {dict.replyingTo} {replyTo.userName}
              </span>
              <button type="button" onClick={() => setReplyTo(null)} className="text-ink-muted hover:text-ink">
                {dict.cancelReply}
              </button>
            </div>
          )}
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder={dict.commentPlaceholder}
            rows={2}
            className="w-full resize-none rounded-lg border border-surface-border bg-surface-hover px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-brand-500 focus:outline-none"
          />
          <div className="flex items-center justify-between gap-2">
            <label className="flex items-center gap-2 text-sm text-ink-muted">
              <input type="checkbox" checked={spoiler} onChange={(e) => setSpoiler(e.target.checked)} />
              {dict.spoilerToggle}
            </label>
            <button
              type="button"
              onClick={post}
              disabled={!text.trim()}
              className="inline-flex items-center gap-1.5 rounded-lg bg-brand-500 px-4 py-1.5 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-50"
            >
              <Send className="h-4 w-4" aria-hidden />
              {dict.sendAction}
            </button>
          </div>
        </div>
      )}

      {topLevel.length === 0 ? (
        <p className="py-4 text-sm text-ink-muted">{dict.commentsEmpty}</p>
      ) : (
        <div className="space-y-4">
          {topLevel.map((parent) => (
            <div key={parent.id} className="space-y-2">
              {renderComment(parent, false)}
              {comments.filter((c) => c.parentId === parent.id).map((reply) => renderComment(reply, true))}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
