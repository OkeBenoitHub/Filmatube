import { RequestsTable } from "@/components/admin/RequestsTable";
import { getDict } from "@/lib/i18n/server";
import { getRequests } from "@/lib/admin/requests";

export default async function AdminRequestsPage() {
  const [dict, requests] = await Promise.all([getDict(), getRequests()]);
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-bold text-ink">{dict.admin.requestsQueueTitle}</h1>
      <RequestsTable requests={requests} dict={dict.admin} />
    </div>
  );
}
