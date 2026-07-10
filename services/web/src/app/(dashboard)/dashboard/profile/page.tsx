import type { Metadata } from "next";

import { ProfileContent } from "@/features/profile/profile-content";

export const metadata: Metadata = { title: "Profile" };

export default function ProfilePage() {
  return <ProfileContent />;
}
