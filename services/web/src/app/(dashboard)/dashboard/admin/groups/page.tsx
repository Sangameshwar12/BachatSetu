import type { Metadata } from "next";

import { AdminGroupsContent } from "@/features/admin/admin-groups-content";

export const metadata: Metadata = { title: "Group Management" };

export default function AdminGroupsPage() {
  return <AdminGroupsContent />;
}
