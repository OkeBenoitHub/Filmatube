import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

/** The two kinds of board — mirrors Android `BoardTypes`. */
export const BOARD_TYPES = { MOVIE: "movie", GENERAL: "general" } as const;
export type BoardType = (typeof BOARD_TYPES)[keyof typeof BOARD_TYPES];

/** A community board (discussion space). Mirrors the Android `Board` model. */
export interface Board {
  id: string;
  title: string;
  description: string;
  coverUrl: string;
  type: string;
  movieId: string;
  movieTitle: string;
  isPublic: boolean;
  isFeatured: boolean;
  isOfficial: boolean;
  ownerId: string;
  memberCount: number;
  createdAtMs: number;
  pinnedMessageId: string;
}

function toMillis(v: unknown): number {
  return (v as { toMillis?: () => number })?.toMillis?.() ?? 0;
}

function mapBoard(id: string, x: FirebaseFirestore.DocumentData): Board {
  return {
    id,
    title: (x.title as string) ?? "",
    description: (x.description as string) ?? "",
    coverUrl: (x.coverUrl as string) ?? "",
    type: (x.type as string) ?? BOARD_TYPES.GENERAL,
    movieId: (x.movieId as string) ?? "",
    movieTitle: (x.movieTitle as string) ?? "",
    isPublic: !!x.isPublic,
    isFeatured: !!x.isFeatured,
    isOfficial: !!x.isOfficial,
    ownerId: (x.ownerId as string) ?? "",
    memberCount: (x.memberCount as number) ?? 0,
    createdAtMs: toMillis(x.createdAt),
    pinnedMessageId: (x.pinnedMessageId as string) ?? "",
  };
}

/** Featured public boards for the discovery header (backed by the isPublic+isFeatured index). */
export async function getFeaturedBoards(limit = 10): Promise<Board[]> {
  const snap = await getAdminDb()
    .collection("boards")
    .where("isPublic", "==", true)
    .where("isFeatured", "==", true)
    .orderBy("memberCount", "desc")
    .limit(limit)
    .get();
  return snap.docs.map((d) => mapBoard(d.id, d.data()));
}

/** Public boards, optionally filtered by type, most-popular first. */
export async function getBoards(type?: string, limit = 50): Promise<Board[]> {
  let query: FirebaseFirestore.Query = getAdminDb().collection("boards").where("isPublic", "==", true);
  if (type) query = query.where("type", "==", type);
  const snap = await query.orderBy("memberCount", "desc").limit(limit).get();
  return snap.docs.map((d) => mapBoard(d.id, d.data()));
}

/** Boards the user owns or has joined, newest first. */
export async function getMyBoards(uid: string, limit = 50): Promise<Board[]> {
  const snap = await getAdminDb()
    .collection("boards")
    .where("memberIds", "array-contains", uid)
    .orderBy("createdAt", "desc")
    .limit(limit)
    .get();
  return snap.docs.map((d) => mapBoard(d.id, d.data()));
}

/** A single board. Returns null when missing, or when it's private and [uid] isn't a member. */
export async function getBoard(id: string, uid?: string): Promise<Board | null> {
  const doc = await getAdminDb().collection("boards").doc(id).get();
  if (!doc.exists) return null;
  const board = mapBoard(doc.id, doc.data() ?? {});
  if (!board.isPublic) {
    const members = (doc.get("memberIds") as string[]) ?? [];
    if (!uid || (board.ownerId !== uid && !members.includes(uid))) return null;
  }
  return board;
}

/** Whether [uid] is a member of [boardId] — mirrors the Android membership check. */
export async function isBoardMember(boardId: string, uid: string): Promise<boolean> {
  const doc = await getAdminDb().collection("boards").doc(boardId).collection("members").doc(uid).get();
  return doc.exists;
}
