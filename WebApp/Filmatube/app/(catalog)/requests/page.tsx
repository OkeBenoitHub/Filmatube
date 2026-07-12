import { RequestForm } from "@/components/requests/RequestForm";
import { getDict } from "@/lib/i18n/server";

export default async function RequestsPage() {
  const dict = await getDict();
  const c = dict.catalog;
  return (
    <div className="mx-auto max-w-2xl px-4 py-8 md:px-6">
      <h1 className="mb-6 text-2xl font-bold text-ink">{c.requestMovie}</h1>
      <RequestForm dict={c} />
    </div>
  );
}
