import type { Metadata } from "next";

import { GroupDetailsContent } from "@/features/groups/group-details-content";

export const metadata: Metadata = { title: "Group Details" };

export default async function GroupDetailsPage({
  params,
}: {
  params: Promise<{ groupId: string }>;
}) {
  const { groupId } = await params;
  return <GroupDetailsContent groupId={groupId} />;
}
