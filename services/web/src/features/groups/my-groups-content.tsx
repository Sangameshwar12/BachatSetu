"use client";

import { Info, Plus, Search, UserPlus, Users } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { PageContainer } from "@/components/dashboard/page-container";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { GroupCard } from "@/features/groups/group-card";
import { isNoActiveGroupError, useMemberDashboard } from "@/hooks/use-member-dashboard";
import { cn } from "@/lib/utils";

export function MyGroupsContent() {
  const { data, isPending, isError, error, refetch } = useMemberDashboard();
  const [search, setSearch] = useState("");

  if (isPending) {
    return (
      <PageContainer title="My Groups">
        <Skeleton className="h-10 w-full max-w-sm rounded-lg" />
        <Skeleton className="h-64 rounded-2xl" />
      </PageContainer>
    );
  }

  if (isError) {
    if (isNoActiveGroupError(error)) {
      return (
        <PageContainer title="My Groups">
          <EmptyState
            icon={Users}
            title="You haven't joined a group yet"
            description="Create your own savings group, or join one with an invitation code, link, or QR."
            action={
              <div className="flex flex-wrap items-center justify-center gap-2">
                <Link href="/dashboard/groups/create" className={cn(buttonVariants())}>
                  <Plus className="size-4" /> Create a group
                </Link>
                <Link href="/dashboard/groups/join" className={cn(buttonVariants({ variant: "outline" }))}>
                  <UserPlus className="size-4" /> Join a group
                </Link>
              </div>
            }
          />
        </PageContainer>
      );
    }
    return (
      <PageContainer title="My Groups">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  const group = data.currentGroup;
  const matchesSearch =
    search.trim().length === 0 ||
    group.name.toLowerCase().includes(search.trim().toLowerCase()) ||
    group.groupCode.toLowerCase().includes(search.trim().toLowerCase());

  return (
    <PageContainer
      title="My Groups"
      description="Every savings group you're a part of."
      actions={
        <div className="flex flex-wrap items-center gap-2">
          <Link href="/dashboard/groups/create" className={cn(buttonVariants())}>
            <Plus className="size-4" /> Create a group
          </Link>
          <Link href="/dashboard/groups/join" className={cn(buttonVariants({ variant: "outline" }))}>
            <UserPlus className="size-4" /> Join a group
          </Link>
        </div>
      }
    >
      <div className="relative max-w-sm">
        <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search by name or code"
          className="pl-9"
        />
      </div>

      {matchesSearch ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <GroupCard
            groupId={group.groupId}
            name={group.name}
            groupCode={group.groupCode}
            contributionAmountPaise={group.upcomingInstallmentAmountPaise}
            currencyCode={group.currencyCode}
            frequency={group.frequency}
            memberCount={group.memberCount}
            maximumMembers={group.maximumMembers}
            nextDrawAt={data.nextDraw?.scheduledAt}
          />
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">No groups match &quot;{search}&quot;.</p>
      )}

      <Alert>
        <Info />
        <AlertTitle>Showing your active group</AlertTitle>
        <AlertDescription>
          The backend currently surfaces one active group per member on this view. Support for
          browsing every group you&apos;ve joined, with filtering and sorting, is on the roadmap.
        </AlertDescription>
      </Alert>
    </PageContainer>
  );
}
