import type { Metadata } from "next";

import { OrganizerSettingsContent } from "@/features/organizer/organizer-settings-content";

export const metadata: Metadata = { title: "Organizer Settings" };

export default function OrganizerSettingsPage() {
  return <OrganizerSettingsContent />;
}
