import type { Metadata } from "next";

import { NotificationsContent } from "@/features/notifications/notifications-content";

export const metadata: Metadata = { title: "Notifications" };

/**
 * Reuses the member Notifications experience unchanged: an organizer is automatically a
 * `GroupMember` of their own group, so `GET /api/v1/dashboard/member` already reflects their
 * own activity — there is no separate organizer-facing notification feed on the backend.
 */
export default function OrganizerNotificationsPage() {
  return <NotificationsContent />;
}
