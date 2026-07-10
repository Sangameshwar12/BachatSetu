import type { Metadata } from "next";

import { AdminTenantsContent } from "@/features/admin/admin-tenants-content";

export const metadata: Metadata = { title: "Tenant Management" };

export default function AdminTenantsPage() {
  return <AdminTenantsContent />;
}
