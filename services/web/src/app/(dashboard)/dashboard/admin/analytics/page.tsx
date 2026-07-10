import type { Metadata } from "next";

import { AdminAnalyticsContent } from "@/features/admin/admin-analytics-content";

export const metadata: Metadata = { title: "Analytics" };

export default function AdminAnalyticsPage() {
  return <AdminAnalyticsContent />;
}
