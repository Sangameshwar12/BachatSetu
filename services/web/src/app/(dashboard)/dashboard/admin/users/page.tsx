import type { Metadata } from "next";

import { AdminUsersContent } from "@/features/admin/admin-users-content";

export const metadata: Metadata = { title: "User Management" };

export default function AdminUsersPage() {
  return <AdminUsersContent />;
}
