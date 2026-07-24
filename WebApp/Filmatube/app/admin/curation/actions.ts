"use server";

import { revalidatePath } from "next/cache";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
}

export interface HomeRowInput {
  titleEn: string;
  titleFr: string;
  movieIds: string[];
  enabled: boolean;
  pinned: boolean;
  /** Epoch millis, or null for "no bound". */
  startAtMs: number | null;
  endAtMs: number | null;
}

const tsOrNull = (ms: number | null) => (ms == null ? null : Timestamp.fromMillis(ms));

/** Create (id null) or update a curated Home row. Returns the id. */
export async function upsertHomeRow(id: string | null, v: HomeRowInput): Promise<string> {
  await assertAdmin();
  const db = getAdminDb();
  const doc = {
    titleEn: v.titleEn.trim(),
    titleFr: v.titleFr.trim(),
    movieIds: v.movieIds,
    enabled: v.enabled,
    pinned: v.pinned,
    startAt: tsOrNull(v.startAtMs),
    endAt: tsOrNull(v.endAtMs),
    updatedAt: FieldValue.serverTimestamp(),
  };

  if (id) {
    await db.collection("homeRows").doc(id).set(doc, { merge: true });
    revalidatePath("/admin/curation");
    return id;
  }

  // New rows go to the end of the current ordering.
  const count = (await db.collection("homeRows").count().get()).data().count;
  const ref = await db.collection("homeRows").add({
    ...doc,
    order: count,
    createdAt: FieldValue.serverTimestamp(),
  });
  revalidatePath("/admin/curation");
  return ref.id;
}

export async function setHomeRowEnabled(id: string, enabled: boolean): Promise<void> {
  await assertAdmin();
  await getAdminDb().collection("homeRows").doc(id).set(
    { enabled, updatedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
  revalidatePath("/admin/curation");
}

export async function deleteHomeRow(id: string): Promise<void> {
  await assertAdmin();
  await getAdminDb().collection("homeRows").doc(id).delete();
  revalidatePath("/admin/curation");
}

/** Persist a new ordering (array of row ids, top to bottom). */
export async function reorderHomeRows(ids: string[]): Promise<void> {
  await assertAdmin();
  const db = getAdminDb();
  const batch = db.batch();
  ids.forEach((id, i) => batch.set(db.collection("homeRows").doc(id), { order: i }, { merge: true }));
  await batch.commit();
  revalidatePath("/admin/curation");
}
