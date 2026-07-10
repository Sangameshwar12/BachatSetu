import type { Metadata } from "next";

import { AdminMonitoringContent } from "@/features/admin/admin-monitoring-content";

export const metadata: Metadata = { title: "Monitoring" };

export default function AdminMonitoringPage() {
  return <AdminMonitoringContent />;
}
