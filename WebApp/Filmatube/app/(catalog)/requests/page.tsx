import { Clapperboard } from "lucide-react";
import { RequestForm } from "@/components/requests/RequestForm";
import { PageHero } from "@/components/ui/PageHero";
import { getDict } from "@/lib/i18n/server";

export default async function RequestsPage() {
  const dict = await getDict();
  const c = dict.catalog;
  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-6">
      <PageHero icon={Clapperboard} eyebrow={c.requestsEyebrow} title={c.requestMovie} subtitle={c.requestsSubtitle} />
      <div className="mx-auto mt-10 max-w-2xl">
        <RequestForm dict={c} />
      </div>
    </div>
  );
}
