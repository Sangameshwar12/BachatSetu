import type { Metadata } from "next";

import { OrganizerGroupDetailsContent } from "@/features/organizer/organizer-group-details-content";

export const metadata: Metadata = { title: "Group Details" };

export default async function OrganizerGroupDetailsPage({
  params,
}: {
  params: Promise<{ groupId: string }>;
}) {
  const { groupId } = await params;
  return <OrganizerGroupDetailsContent groupId={groupId} />;
}
