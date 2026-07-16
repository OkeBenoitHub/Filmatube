import { ActivityFeed } from "@/components/social/ActivityFeed";
import { MyPartiesRow } from "@/components/parties/MyPartiesRow";
import { getCurrentUser } from "@/lib/auth/session";
import { getMyParties } from "@/lib/parties";
import { getDict } from "@/lib/i18n/server";

export default async function ActivityPage() {
  const [dict, user] = await Promise.all([getDict(), getCurrentUser()]);
  const parties = user ? await getMyParties(user.uid) : [];

  return (
    <>
      <ActivityFeed dict={dict.catalog} />
      {parties.length > 0 && (
        <div className="mx-auto max-w-6xl px-4 pb-8 md:px-6">
          <MyPartiesRow parties={parties} dict={dict.catalog} />
        </div>
      )}
    </>
  );
}
