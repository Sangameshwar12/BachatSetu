import type { Metadata } from "next";

import { OrganizerDashboardContent } from "@/features/organizer/organizer-dashboard-content";

export const metadata: Metadata = { title: "Organizer Dashboard" };

export default function OrganizerDashboardPage() {
  return <OrganizerDashboardContent />;
}
