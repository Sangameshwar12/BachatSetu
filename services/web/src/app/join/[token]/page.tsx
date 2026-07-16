import type { Metadata } from "next";

import { JoinPreviewContent } from "@/features/groups/join-preview-content";

export const metadata: Metadata = { title: "Join a Group" };

export default async function JoinTokenPage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  return <JoinPreviewContent token={token} />;
}
