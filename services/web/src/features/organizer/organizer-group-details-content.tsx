"use client";

import { FileText, MessageCircle, UserPlus } from "lucide-react";
import Link from "next/link";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { PageContainer } from "@/components/dashboard/page-container";
import { Button, buttonVariants } from "@/components/ui/button";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/contexts/auth-context";
import { GroupMembersTab } from "@/features/groups/group-members-tab";
import { GroupOverviewTab } from "@/features/groups/group-overview-tab";
import { GroupTabPlaceholder } from "@/features/groups/group-tab-placeholder";
import { OrganizerGroupDrawsTab } from "@/features/organizer/organizer-group-draws-tab";
import { OrganizerGroupPaymentsTab } from "@/features/organizer/organizer-group-payments-tab";
import { OrganizerGroupSettingsTab } from "@/features/organizer/organizer-group-settings-tab";
import { useGroup, useRemoveMember } from "@/hooks/use-group";
import { isNoActiveInvitationError, useCurrentInvitation } from "@/hooks/use-group-invitation";
import { useOrganizerDashboard } from "@/hooks/use-organizer-dashboard";
import { ApiError } from "@/services/api-client";
import { cn } from "@/lib/utils";
import { buildInvitationShareMessage, shareViaWhatsApp } from "@/utils/share";

export function OrganizerGroupDetailsContent({ groupId }: { groupId: string }) {
  const { session } = useAuth();
  const { data: group, isPending, isError, error, refetch } = useGroup(groupId);
  const organizerDashboard = useOrganizerDashboard();
  const organizerGroup = organizerDashboard.data?.groups.find((g) => g.groupId === groupId);
  const invitation = useCurrentInvitation(groupId);
  const removeMember = useRemoveMember(groupId);

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

  const currentGroup = group;
  const isOwner = session?.userId === currentGroup.ownerId;

  async function handleRemoveMember(memberId: string) {
    try {
      await removeMember.mutateAsync(memberId);
      toast.success("Member removed.");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't remove this member.");
    }
  }

  function handleShare() {
    if (!invitation.data) {
      toast.error("Generate an invitation first, then share it.");
      return;
    }
    const fullJoinLink =
      typeof window !== "undefined"
        ? `${window.location.origin}${invitation.data.joinLink}`
        : invitation.data.joinLink;
    shareViaWhatsApp(
      buildInvitationShareMessage({
        groupName: currentGroup.name,
        inviteCode: invitation.data.code,
        inviteLink: fullJoinLink,
      })
    );
    toast.success("Invitation shared");
  }

  return (
    <PageContainer
      title={group.name}
      description={group.groupCode}
      actions={
        <div className="flex flex-wrap items-center gap-2">
          {organizerGroup?.hasActiveInvitation && (
            <Badge variant="outline" className="border-transparent bg-info/10 text-info">
              Invitation pending
            </Badge>
          )}
          {isOwner && (
            <>
              <Button
                variant="outline"
                onClick={handleShare}
                disabled={
                  invitation.isPending || (invitation.isError && !isNoActiveInvitationError(invitation.error))
                }
              >
                <MessageCircle className="size-4" /> Share
              </Button>
              <Link
                href={`/dashboard/organizer/groups/${groupId}/invite`}
                className={cn(buttonVariants())}
              >
                <UserPlus className="size-4" /> Invite members
              </Link>
            </>
          )}
        </div>
      }
    >
      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="members">Members</TabsTrigger>
          <TabsTrigger value="payments">Collection</TabsTrigger>
          <TabsTrigger value="draws">Draws</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
          <TabsTrigger value="settings">Settings</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="pt-4">
          <GroupOverviewTab group={group} />
        </TabsContent>

        <TabsContent value="members" className="pt-4">
          <GroupMembersTab
            group={group}
            currentUserId={session?.userId}
            onRemove={isOwner ? handleRemoveMember : undefined}
            removingMemberId={removeMember.isPending ? removeMember.variables : undefined}
          />
        </TabsContent>

        <TabsContent value="payments" className="pt-4">
          <OrganizerGroupPaymentsTab groupId={groupId} isOwner={isOwner} />
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
          {isOwner ? (
            <OrganizerGroupSettingsTab group={group} />
          ) : (
            <GroupTabPlaceholder
              icon={FileText}
              title="Organizer settings"
              description="Only the group's organizer can activate, suspend, or close this group."
            />
          )}
        </TabsContent>
      </Tabs>
    </PageContainer>
  );
}
