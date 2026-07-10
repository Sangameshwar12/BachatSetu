import type { Metadata } from "next";

import { InviteMembersContent } from "@/features/organizer/invite-members-content";

export const metadata: Metadata = { title: "Invite Members" };

export default async function InviteMembersPage({
  params,
}: {
  params: Promise<{ groupId: string }>;
}) {
  const { groupId } = await params;
  return <InviteMembersContent groupId={groupId} />;
}
