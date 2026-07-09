"use server";

import { revalidatePath } from "next/cache";
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

export async function createCollection(): Promise<string> {
  const user = await requireUser();
  const ref = await getAdminDb().collection("collections").add({
    userId: user.uid,
    title: "Untitled",
    coverUrl: "",
    isPublic: false,
    createdAt: FieldValue.serverTimestamp(),
  });
  revalidatePath("/collections");
  return ref.id;
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
    addedAt: FieldValue.serverTimestamp(),
  });
  revalidatePath(`/collections/${id}`);
}

export async function removeMovieFromCollection(id: string, movieId: string): Promise<void> {
  const user = await requireUser();
  await assertOwner(id, user.uid);
  await getAdminDb().collection("collections").doc(id).collection("items").doc(movieId).delete();
  revalidatePath(`/collections/${id}`);
}
