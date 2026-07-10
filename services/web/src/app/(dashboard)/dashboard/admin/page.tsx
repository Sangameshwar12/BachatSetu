import type { Metadata } from "next";

import { AdminDashboardContent } from "@/features/admin/admin-dashboard-content";

export const metadata: Metadata = { title: "Platform Dashboard" };

export default function AdminDashboardPage() {
  return <AdminDashboardContent />;
}
