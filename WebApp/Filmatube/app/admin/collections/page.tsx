import { EditorialManager } from "@/components/admin/EditorialManager";
import { listEditorialCollections } from "@/lib/admin/editorial";
import { getDict } from "@/lib/i18n/server";

/**
 * Admin editorial collections: hand-curated, themed collections that can be featured on Home.
 * Content (title, cover, movies) is edited through the shared /collections/[id] editor — the
 * admin owns the collection — while feature/order/subtitle live here.
 */
export default async function AdminCollectionsPage() {
  const dict = await getDict();
  const rows = await listEditorialCollections();
  return <EditorialManager rows={rows} dict={dict.adminEditorial} />;
}
