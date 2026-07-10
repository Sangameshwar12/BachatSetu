import type { Metadata } from "next";

import { OrganizerPaymentsContent } from "@/features/organizer/organizer-payments-content";

export const metadata: Metadata = { title: "Payments" };

export default function OrganizerPaymentsPage() {
  return <OrganizerPaymentsContent />;
}
