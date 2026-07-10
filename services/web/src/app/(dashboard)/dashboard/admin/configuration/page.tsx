import type { Metadata } from "next";

import { AdminConfigurationContent } from "@/features/admin/admin-configuration-content";

export const metadata: Metadata = { title: "Platform Configuration" };

export default function AdminConfigurationPage() {
  return <AdminConfigurationContent />;
}
