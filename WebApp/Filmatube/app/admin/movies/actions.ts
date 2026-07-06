"use server";

import { revalidatePath } from "next/cache";
import { FieldValue } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";
import type { MovieFormValues } from "@/lib/admin/movie-form";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
}

function toDoc(v: MovieFormValues) {
  return {
    type: "movie",
    imdbId: v.imdbId,
    tmdbId: v.tmdbId,
    title: { en: v.titleEn, fr: v.titleFr },
    description: { en: v.descEn, fr: v.descFr },
    posterUrl: v.posterUrl,
    backdropUrl: v.backdropUrl,
    thumbnailUrl: v.posterUrl,
    trailerUrl: v.trailerUrl,
    videoKey: v.videoKey,
    genres: v.genres,
    year: Number(v.year) || 0,
    duration: Number(v.duration) || 0,
    ageRating: v.ageRating,
    cast: v.cast,
    directors: v.directors,
    language: "en",
    status: v.status,
    isFeatured: v.isFeatured,
    isPinned: v.isPinned,
    isComingSoon: v.isComingSoon,
    updatedAt: FieldValue.serverTimestamp(),
  };
}

/** Create (id null) or update a movie. Returns the id. */
export async function upsertMovie(id: string | null, values: MovieFormValues): Promise<string> {
  await assertAdmin();
  const db = getAdminDb();
  const doc = toDoc(values);

  if (id) {
    await db.collection("movies").doc(id).set(doc, { merge: true });
    revalidatePath("/admin/movies");
    return id;
  }

  const ref = await db.collection("movies").add({
    ...doc,
    subtitleTracks: [],
    audioTracks: [],
    averageRating: 0,
    ratingsCount: 0,
    likesCount: 0,
    viewsCount: 0,
    addedAt: FieldValue.serverTimestamp(),
  });
  revalidatePath("/admin/movies");
  return ref.id;
}

export async function deleteMovie(id: string): Promise<void> {
  await assertAdmin();
  await getAdminDb().collection("movies").doc(id).delete();
  revalidatePath("/admin/movies");
}
