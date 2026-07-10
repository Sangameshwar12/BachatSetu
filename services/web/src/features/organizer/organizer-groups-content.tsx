"use client";

import { Search, Users } from "lucide-react";
import { useState } from "react";

import { PageContainer } from "@/components/dashboard/page-container";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { GroupCard } from "@/features/groups/group-card";
import { useOrganizerDashboard } from "@/hooks/use-organizer-dashboard";

export function OrganizerGroupsContent() {
  const { data, isPending, isError, error, refetch } = useOrganizerDashboard();
  const [search, setSearch] = useState("");

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
            />
          ))}
        </div>
      )}
    </PageContainer>
  );
}
