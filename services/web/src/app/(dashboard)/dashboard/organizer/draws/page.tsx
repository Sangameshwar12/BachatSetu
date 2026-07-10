import type { Metadata } from "next";

import { OrganizerDrawsContent } from "@/features/organizer/organizer-draws-content";

export const metadata: Metadata = { title: "Draws" };

export default function OrganizerDrawsPage() {
  return <OrganizerDrawsContent />;
}
