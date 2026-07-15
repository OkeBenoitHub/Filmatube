"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { FieldValue } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";

async function requireUser() {
  const user = await getCurrentUser();
  if (!user) throw new Error("Unauthorized");
  return user;
}

async function assertOwner(id: string, uid: string) {
  const doc = await getAdminDb().collection("collections").doc(id).get();
  if (!doc.exists || doc.get("userId") !== uid) throw new Error("Forbidden");
}

/** Creates a collection and redirects to its editor in the SAME action round-trip. */
export async function createCollection(): Promise<never> {
  const user = await requireUser();
  const ref = await getAdminDb().collection("collections").add({
    userId: user.uid,
    title: "Untitled",
    coverUrl: "",
    isPublic: false,
    createdAt: FieldValue.serverTimestamp(),
  });
  revalidatePath("/collections");
  // redirect() throws NEXT_REDIRECT — the client transition navigates without a second round-trip.
  redirect(`/collections/${ref.id}`);
}

export async function saveCollection(
  id: string,
  values: { title: string; coverUrl: string; isPublic: boolean },
): Promise<void> {
  const user = await requireUser();
  await assertOwner(id, user.uid);
  await getAdminDb().collection("collections").doc(id).set(
    {
      title: values.title,
      coverUrl: values.coverUrl,
      isPublic: values.isPublic,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
  revalidatePath("/collections");
  revalidatePath(`/collections/${id}`);
}

export async function deleteCollection(id: string): Promise<void> {
  const user = await requireUser();
  await assertOwner(id, user.uid);
  await getAdminDb().collection("collections").doc(id).delete();
  revalidatePath("/collections");
}

export async function addMovieToCollection(id: string, movieId: string): Promise<void> {
  const user = await requireUser();
  await assertOwner(id, user.uid);
  await getAdminDb().collection("collections").doc(id).collection("items").doc(movieId).set({
    movieId,
    order: Date.now(),
    addedAt: FieldValue.serverTimestamp(),
  });
  revalidatePath(`/collections/${id}`);
}

/** Move an item up/down by swapping its order with the adjacent item. */
export async function moveCollectionItem(id: string, movieId: string, direction: "up" | "down"): Promise<void> {
  const user = await requireUser();
  await assertOwner(id, user.uid);
  const col = getAdminDb().collection("collections").doc(id).collection("items");
  const snap = await col.get();
  const items = snap.docs
    .map((d) => ({
      id: d.id,
      order: (d.get("order") as number) ?? (d.get("addedAt") as { toMillis?: () => number })?.toMillis?.() ?? 0,
    }))
    .sort((a, b) => a.order - b.order);

  const idx = items.findIndex((i) => i.id === movieId);
  const swapIdx = direction === "up" ? idx - 1 : idx + 1;
  if (idx < 0 || swapIdx < 0 || swapIdx >= items.length) return;

  const batch = getAdminDb().batch();
  batch.set(col.doc(items[idx].id), { order: items[swapIdx].order }, { merge: true });
  batch.set(col.doc(items[swapIdx].id), { order: items[idx].order }, { merge: true });
  await batch.commit();
  revalidatePath(`/collections/${id}`);
}

export async function removeMovieFromCollection(id: string, movieId: string): Promise<void> {
  const user = await requireUser();
  await assertOwner(id, user.uid);
  await getAdminDb().collection("collections").doc(id).collection("items").doc(movieId).delete();
  revalidatePath(`/collections/${id}`);
}

/** Clone a public collection (title + cover + items) into the current user's collections. */
export async function saveCollectionCopy(sourceId: string): Promise<string> {
  const user = await requireUser();
  const db = getAdminDb();
  const source = await db.collection("collections").doc(sourceId).get();
  if (!source.exists) throw new Error("Not found");
  if (source.get("userId") !== user.uid && source.get("isPublic") !== true) throw new Error("Forbidden");

  const ref = await db.collection("collections").add({
    userId: user.uid,
    title: `${source.get("title") ?? "Untitled"}`,
    coverUrl: source.get("coverUrl") ?? "",
    isPublic: false,
    copiedFrom: sourceId,
    createdAt: FieldValue.serverTimestamp(),
  });

  const items = await db.collection("collections").doc(sourceId).collection("items").get();
  const batch = db.batch();
  items.docs.forEach((item) => {
    batch.set(ref.collection("items").doc(item.id), {
      movieId: item.id,
      addedAt: FieldValue.serverTimestamp(),
    });
  });
  await batch.commit();

  revalidatePath("/collections");
  return ref.id;
}
