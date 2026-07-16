import { UsersTable } from "@/components/admin/UsersTable";
import { getAdminUsers } from "@/lib/admin/users";
import { getDict } from "@/lib/i18n/server";

export default async function AdminUsersPage() {
  const [dict, users] = await Promise.all([getDict(), getAdminUsers()]);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-ink">{dict.admin.usersTitle}</h1>
        <p className="mt-1 text-sm text-ink-muted">{dict.admin.usersSubtitle}</p>
      </div>
      <UsersTable users={users} dict={dict.admin} />
    </div>
  );
}
