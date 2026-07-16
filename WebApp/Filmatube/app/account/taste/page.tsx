import { AccountHeader } from "@/components/account/AccountHeader";
import { TasteForm } from "@/components/account/TasteForm";
import { requireUser } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";

export default async function TastePage() {
  await requireUser();
  const dict = await getDict();
  return (
    <div className="min-h-screen">
      <AccountHeader signOutLabel={dict.account.signOut} />
      <TasteForm dict={dict.taste} genres={dict.genres} />
    </div>
  );
}
