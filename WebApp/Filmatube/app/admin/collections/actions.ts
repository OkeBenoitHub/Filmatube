"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { FieldValue } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
  return user;
}

/**
 * Create an editorial collection and jump straight to its editor. It's owned by the admin who
 * made it, so the existing /collections/[id] editor and item actions (which assert ownership)
 * work unchanged for adding a cover, title and movies. `isPublic` so any viewer can open it.
 */
export async function createEditorialCollection(): Promise<never> {
  const user = await assertAdmin();
  const ref = await getAdminDb().collection("collections").add({
    userId: user.uid,
    title: "Untitled collection",
    subtitle: "",
    coverUrl: "",
    isPublic: true,
    isEditorial: true,
    featured: false,
    featuredOrder: 0,
    createdAt: FieldValue.serverTimestamp(),
  });
  revalidatePath("/admin/collections");
  redirect(`/collections/${ref.id}`);
}

export async function setCollectionSubtitle(id: string, subtitle: string): Promise<void> {
  await assertAdmin();
  await getAdminDb().collection("collections").doc(id).set(
    { subtitle: subtitle.trim(), updatedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
  revalidatePath("/admin/collections");
}

/** Feature / unfeature on Home. New features go to the end of the current order. */
export async function setCollectionFeatured(id: string, featured: boolean): Promise<void> {
  await assertAdmin();
  const db = getAdminDb();
  let featuredOrder = 0;
  if (featured) {
    featuredOrder = (await db.collection("collections").where("featured", "==", true).count().get()).data().count;
  }
  await db.collection("collections").doc(id).set(
    { featured, featuredOrder, updatedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
  revalidatePath("/admin/collections");
  revalidatePath("/home");
}

/** Persist a new featured ordering (row ids, top to bottom). */
export async function reorderFeatured(ids: string[]): Promise<void> {
  await assertAdmin();
  const db = getAdminDb();
  const batch = db.batch();
  ids.forEach((id, i) => batch.set(db.collection("collections").doc(id), { featuredOrder: i }, { merge: true }));
  await batch.commit();
  revalidatePath("/admin/collections");
  revalidatePath("/home");
}

/** Delete an editorial collection and its items. */
export async function deleteEditorialCollection(id: string): Promise<void> {
  await assertAdmin();
  const db = getAdminDb();
  const items = await db.collection("collections").doc(id).collection("items").get();
  const batch = db.batch();
  items.docs.forEach((d) => batch.delete(d.ref));
  batch.delete(db.collection("collections").doc(id));
  await batch.commit();
  revalidatePath("/admin/collections");
  revalidatePath("/home");
}
