"use client";

import { PageContainer } from "@/components/dashboard/page-container";
import { AdminAnnouncementsContent } from "@/features/admin/admin-announcements-content";
import { AdminBroadcastForm } from "@/features/admin/admin-broadcast-form";

export function AdminSupportContent() {
  return (
    <PageContainer title="Support" description="Broadcast notifications and platform announcements.">
      <AdminBroadcastForm />
      <AdminAnnouncementsContent />
    </PageContainer>
  );
}
