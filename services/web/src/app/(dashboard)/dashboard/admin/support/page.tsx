import type { Metadata } from "next";

import { AdminSupportContent } from "@/features/admin/admin-support-content";

export const metadata: Metadata = { title: "Support" };

export default function AdminSupportPage() {
  return <AdminSupportContent />;
}
