import Link from "next/link";
import { Globe } from "lucide-react";
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
            <Link key={col.id} href={`/collections/${col.id}`} className="group block">
              <div className="relative aspect-video overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
                {col.coverUrl && (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={col.coverUrl} alt="" className="h-full w-full object-cover" />
                )}
                {col.isPublic && (
                  <span className="absolute right-1.5 top-1.5 rounded bg-black/60 p-1 text-white">
                    <Globe className="h-3.5 w-3.5" aria-hidden />
                  </span>
                )}
              </div>
              <p className="mt-1.5 truncate text-sm font-medium text-ink">{col.title}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
