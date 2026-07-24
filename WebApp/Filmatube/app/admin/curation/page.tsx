import { CurationManager } from "@/components/admin/CurationManager";
import { listHomeRows } from "@/lib/admin/home-rows";
import { getDict } from "@/lib/i18n/server";

/**
 * Admin curation: build manual Home rows, pin them above the personalised rails, and schedule
 * them as campaigns with a start/end window. Rendered on Home by HomeClient.
 */
export default async function AdminCurationPage() {
  const dict = await getDict();
  const rows = await listHomeRows();
  return <CurationManager rows={rows} dict={dict.adminCuration} />;
}
