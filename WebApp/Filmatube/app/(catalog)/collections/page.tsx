import { Library } from "lucide-react";
import { PageHero } from "@/components/ui/PageHero";
import { CollectionsGrid } from "@/components/collections/CollectionsGrid";
import { NewCollectionButton } from "@/components/collections/NewCollectionButton";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict } from "@/lib/i18n/server";
import { getUserCollections } from "@/lib/collections";

export default async function CollectionsPage() {
  const user = await getCurrentUser();
  const [dict, collections] = await Promise.all([
    getDict(),
    user ? getUserCollections(user.uid) : Promise.resolve([]),
  ]);
  const c = dict.catalog;

  return (
    <div className="mx-auto max-w-6xl space-y-8 px-4 py-8 md:px-6">
      <PageHero icon={Library} eyebrow={c.libraryTitle} title={c.collections} subtitle={c.collectionsSubtitle}>
        <NewCollectionButton label={c.newCollection} />
      </PageHero>

      {collections.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-surface-border py-16 text-center">
          <p className="text-ink-muted">{c.noCollections}</p>
        </div>
      ) : (
        <CollectionsGrid collections={collections} dict={c} />
      )}
    </div>
  );
}
