import type { Metadata } from "next";
import { Suspense } from "react";

import { JoinGroupContent } from "@/features/groups/join-group-content";

export const metadata: Metadata = { title: "Join a Group" };

export default function JoinGroupPage() {
  return (
    <Suspense>
      <JoinGroupContent />
    </Suspense>
  );
}
