import type { Metadata } from "next";

import { NotificationsContent } from "@/features/notifications/notifications-content";

export const metadata: Metadata = { title: "Notifications" };

export default function NotificationsPage() {
  return <NotificationsContent />;
}
