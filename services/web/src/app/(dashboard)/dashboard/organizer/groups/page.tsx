import type { Metadata } from "next";

import { OrganizerGroupsContent } from "@/features/organizer/organizer-groups-content";

export const metadata: Metadata = { title: "My Groups" };

export default function OrganizerGroupsPage() {
  return <OrganizerGroupsContent />;
}
