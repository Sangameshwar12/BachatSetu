import type { Metadata } from "next";

import { MyGroupsContent } from "@/features/groups/my-groups-content";

export const metadata: Metadata = { title: "My Groups" };

export default function MyGroupsPage() {
  return <MyGroupsContent />;
}
