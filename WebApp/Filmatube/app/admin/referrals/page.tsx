import { ReferralsDashboard } from "@/components/admin/ReferralsDashboard";
import { getReferralAnalytics } from "@/lib/admin/referrals";
import { getDict } from "@/lib/i18n/server";

/** Admin referral analytics + abuse review (Day 206). */
export default async function AdminReferralsPage() {
  const [dict, analytics] = await Promise.all([getDict(), getReferralAnalytics()]);
  return <ReferralsDashboard analytics={analytics} dict={dict.adminReferrals} />;
}
