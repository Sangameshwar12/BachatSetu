"use client";

import { FileText, Gavel, Wallet } from "lucide-react";

import { PageContainer } from "@/components/dashboard/page-container";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/contexts/auth-context";
import { GroupMembersTab } from "@/features/groups/group-members-tab";
import { GroupOverviewTab } from "@/features/groups/group-overview-tab";
import { GroupTabPlaceholder } from "@/features/groups/group-tab-placeholder";
import { useGroup } from "@/hooks/use-group";

export function GroupDetailsContent({ groupId }: { groupId: string }) {
  const { session } = useAuth();
  const { data: group, isPending, isError, error, refetch } = useGroup(groupId);

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
    <PageContainer title={group.name} description={group.groupCode}>
      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="members">Members</TabsTrigger>
          <TabsTrigger value="payments">Payments</TabsTrigger>
          <TabsTrigger value="draws">Draw History</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="pt-4">
          <GroupOverviewTab group={group} />
        </TabsContent>

        <TabsContent value="members" className="pt-4">
          <GroupMembersTab group={group} currentUserId={session?.userId} />
        </TabsContent>

        <TabsContent value="payments" className="pt-4">
          <GroupTabPlaceholder
            icon={Wallet}
            title="Group payment history isn't available yet"
            description="The Payments API doesn't yet support filtering by group, so a per-group payment history can't be shown here without exposing other members' data."
          />
        </TabsContent>

        <TabsContent value="draws" className="pt-4">
          <GroupTabPlaceholder
            icon={Gavel}
            title="Draw history isn't available yet"
            description="The Draws API doesn't yet support filtering by group, so past winners and draw dates for this specific group can't be listed here yet."
          />
        </TabsContent>

        <TabsContent value="documents" className="pt-4">
          <GroupTabPlaceholder
            icon={FileText}
            title="No documents yet"
            description="Group rule documents and receipts will appear here once the backend links stored files to a group."
          />
        </TabsContent>
      </Tabs>
    </PageContainer>
  );
}
