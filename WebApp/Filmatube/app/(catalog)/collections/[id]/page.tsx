import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { CollectionEditor } from "@/components/collections/CollectionEditor";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getCollection } from "@/lib/collections";

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params;
  const data = await getCollection(id);
  if (!data || !data.collection.isPublic) return {};
  const { title, coverUrl } = data.collection;
  return {
    title,
    openGraph: {
      title,
      url: `/collections/${id}`,
      type: "website",
      images: coverUrl ? [{ url: coverUrl }] : [],
    },
    twitter: { card: "summary_large_image", title, images: coverUrl ? [coverUrl] : [] },
  };
}

export default async function CollectionPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [user, data, locale, dict] = await Promise.all([
    getCurrentUser(),
    getCollection(id),
    getLocale(),
    getDict(),
  ]);
  if (!data) notFound();

  const isOwner = user?.uid === data.collection.userId;
  if (!isOwner && !data.collection.isPublic) notFound();

  return (
    <CollectionEditor
      collection={data.collection}
      movies={data.movies}
      locale={locale}
      dict={dict.catalog}
      isOwner={isOwner}
    />
  );
}
