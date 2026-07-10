"use client";

import { FileText, UserPlus, Users } from "lucide-react";
import Link from "next/link";

import { PageContainer } from "@/components/dashboard/page-container";
import { buttonVariants } from "@/components/ui/button";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { GroupOverviewTab } from "@/features/groups/group-overview-tab";
import { GroupTabPlaceholder } from "@/features/groups/group-tab-placeholder";
import { OrganizerGroupDrawsTab } from "@/features/organizer/organizer-group-draws-tab";
import { OrganizerGroupPaymentsTab } from "@/features/organizer/organizer-group-payments-tab";
import { OrganizerGroupSettingsTab } from "@/features/organizer/organizer-group-settings-tab";
import { useGroup } from "@/hooks/use-group";
import { useOrganizerDashboard } from "@/hooks/use-organizer-dashboard";
import { cn } from "@/lib/utils";

export function OrganizerGroupDetailsContent({ groupId }: { groupId: string }) {
  const { data: group, isPending, isError, error, refetch } = useGroup(groupId);
  const organizerDashboard = useOrganizerDashboard();
  const organizerGroup = organizerDashboard.data?.groups.find((g) => g.groupId === groupId);

  if (isPending) {
    return (
      <PageContainer title="Group details">
        <Skeleton className="h-96 rounded-2xl" />
      </PageContainer>
    );
  }

  if (isError) {
    return (
      <PageContainer title="Group details">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title={group.name}
      description={group.groupCode}
      actions={
        <Link
          href={`/dashboard/organizer/groups/${groupId}/invite`}
          className={cn(buttonVariants())}
        >
          <UserPlus className="size-4" /> Invite members
        </Link>
      }
    >
      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="members">Members</TabsTrigger>
          <TabsTrigger value="payments">Payments</TabsTrigger>
          <TabsTrigger value="draws">Draws</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
          <TabsTrigger value="settings">Settings</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="pt-4">
          <GroupOverviewTab group={group} />
        </TabsContent>

        <TabsContent value="members" className="pt-4">
          <GroupTabPlaceholder
            icon={Users}
            title="Member list isn't available yet"
            description="Viewing every member's status and join date requires a group-scoped member list endpoint that doesn't exist on the backend yet — only a total member count is exposed today."
          />
        </TabsContent>

        <TabsContent value="payments" className="pt-4">
          <OrganizerGroupPaymentsTab group={organizerGroup} />
        </TabsContent>

        <TabsContent value="draws" className="pt-4">
          <OrganizerGroupDrawsTab group={organizerGroup} />
        </TabsContent>

        <TabsContent value="documents" className="pt-4">
          <GroupTabPlaceholder
            icon={FileText}
            title="No documents yet"
            description="Group rule documents and receipts will appear here once the backend links stored files to a group."
          />
        </TabsContent>

        <TabsContent value="settings" className="pt-4">
          <OrganizerGroupSettingsTab group={group} />
        </TabsContent>
      </Tabs>
    </PageContainer>
  );
}
