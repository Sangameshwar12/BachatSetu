import type { Metadata } from "next";

import { CreateGroupContent } from "@/features/groups/create-group-content";

export const metadata: Metadata = { title: "Create a Group" };

export default function CreateGroupPage() {
  return <CreateGroupContent />;
}
