import { CollectionCard } from "@/components/collections/CollectionCard";
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
    <div className="mx-auto max-w-6xl space-y-6 px-4 py-8 md:px-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-ink">{c.collections}</h1>
        <NewCollectionButton label={c.newCollection} />
      </div>

      {collections.length === 0 ? (
        <p className="py-16 text-center text-ink-muted">{c.noCollections}</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
          {collections.map((col) => (
            <CollectionCard key={col.id} collection={col} isOwner />
          ))}
        </div>
      )}
    </div>
  );
}
