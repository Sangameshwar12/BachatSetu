"use client";

import { Search, Users } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";

import { PageContainer } from "@/components/dashboard/page-container";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { GroupCard } from "@/features/groups/group-card";
import { invitationQueryKey } from "@/hooks/use-group-invitation";
import { useOrganizerDashboard } from "@/hooks/use-organizer-dashboard";
import { ApiError } from "@/services/api-client";
import { getCurrentInvitation } from "@/services/invitation-service";
import type { OrganizerGroupResponse } from "@/types/organizer-dashboard";
import { buildInvitationShareMessage, shareViaWhatsApp } from "@/utils/share";

export function OrganizerGroupsContent() {
  const { data, isPending, isError, error, refetch } = useOrganizerDashboard();
  const [search, setSearch] = useState("");
  const queryClient = useQueryClient();

  async function handleShare(group: OrganizerGroupResponse) {
    if (!group.hasActiveInvitation) {
      toast.error("Generate an invitation first, then share it.");
      return;
    }
    try {
      const invitation = await queryClient.fetchQuery({
        queryKey: invitationQueryKey(group.groupId),
        queryFn: () => getCurrentInvitation(group.groupId),
      });
      const fullJoinLink = `${window.location.origin}${invitation.joinLink}`;
      shareViaWhatsApp(
        buildInvitationShareMessage({
          groupName: group.name,
          inviteCode: invitation.code,
          inviteLink: fullJoinLink,
        })
      );
      toast.success("Invitation shared");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't share this invitation.");
    }
  }

  if (isPending) {
    return (
      <PageContainer title="My Groups">
        <Skeleton className="h-10 w-full max-w-sm rounded-lg" />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-64 rounded-2xl" />
          <Skeleton className="h-64 rounded-2xl" />
          <Skeleton className="h-64 rounded-2xl" />
        </div>
      </PageContainer>
    );
  }

  if (isError) {
    return (
      <PageContainer title="My Groups">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  const query = search.trim().toLowerCase();
  const filtered = data.groups.filter(
    (group) =>
      query.length === 0 ||
      group.name.toLowerCase().includes(query) ||
      group.groupCode.toLowerCase().includes(query)
  );

  return (
    <PageContainer title="My Groups" description="Every group you organize.">
      <div className="relative max-w-sm">
        <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search by name or code"
          className="pl-9"
        />
      </div>

      {data.groups.length === 0 ? (
        <EmptyState
          icon={Users}
          title="You don't organize any groups yet"
          description="Groups you create or already own will appear here."
        />
      ) : filtered.length === 0 ? (
        <p className="text-sm text-muted-foreground">No groups match &quot;{search}&quot;.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((group) => (
            <GroupCard
              key={group.groupId}
              groupId={group.groupId}
              name={group.name}
              groupCode={group.groupCode}
              memberCount={group.memberCount}
              maximumMembers={group.maximumMembers}
              nextDrawAt={group.nextDraw?.scheduledAt}
              contributionProgressPercent={group.contributionProgressPercent}
              detailsHref={`/dashboard/organizer/groups/${group.groupId}`}
              hasActiveInvitation={group.hasActiveInvitation}
              inviteHref={`/dashboard/organizer/groups/${group.groupId}/invite`}
              onShare={() => handleShare(group)}
            />
          ))}
        </div>
      )}
    </PageContainer>
  );
}
