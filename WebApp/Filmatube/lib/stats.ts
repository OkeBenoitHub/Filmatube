import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

/** A user's rolled-up stats (`stats/{uid}`, built nightly by `buildStats`). */
export interface UserStats {
  totalWatchMinutes: number;
  moviesCompleted: number;
  reviewsWritten: number;
  topGenres: string[];
  /** Light gamification (Day 202). */
  currentStreak: number;
  longestStreak: number;
  weeklyCompleted: number;
  weeklyGoal: number;
}

const EMPTY: UserStats = {
  totalWatchMinutes: 0,
  moviesCompleted: 0,
  reviewsWritten: 0,
  topGenres: [],
  currentStreak: 0,
  longestStreak: 0,
  weeklyCompleted: 0,
  weeklyGoal: 3,
};

export async function getUserStats(uid: string): Promise<UserStats> {
  const doc = await getAdminDb().collection("stats").doc(uid).get();
  if (!doc.exists) return EMPTY;
  return {
    totalWatchMinutes: (doc.get("totalWatchMinutes") as number) ?? 0,
    moviesCompleted: (doc.get("moviesCompleted") as number) ?? 0,
    reviewsWritten: (doc.get("reviewsWritten") as number) ?? 0,
    topGenres: ((doc.get("topGenres") as string[]) ?? []).slice(0, 3),
    currentStreak: (doc.get("currentStreak") as number) ?? 0,
    longestStreak: (doc.get("longestStreak") as number) ?? 0,
    weeklyCompleted: (doc.get("weeklyCompleted") as number) ?? 0,
    weeklyGoal: (doc.get("weeklyGoal") as number) || 3,
  };
}

/** Whole hours watched — the headline number; minutes below an hour read as 0. */
export function watchHours(stats: UserStats): number {
  return Math.floor(stats.totalWatchMinutes / 60);
}
