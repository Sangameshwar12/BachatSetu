"use client";

import { Bell, Gavel, Settings, Users, Wallet } from "lucide-react";
import Link from "next/link";

import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { GroupCard } from "@/features/groups/group-card";
import { RecentNotificationsCard } from "@/features/dashboard/recent-notifications-card";
import { StatTile } from "@/features/dashboard/stat-tile";
import { useMemberDashboard } from "@/hooks/use-member-dashboard";
import { useOrganizerDashboard } from "@/hooks/use-organizer-dashboard";
import { cn } from "@/lib/utils";
import { formatDateTime } from "@/utils/format";

const organizerQuickActions = [
  { label: "My Groups", href: "/dashboard/organizer/groups", icon: Users },
  { label: "Payments", href: "/dashboard/organizer/payments", icon: Wallet },
  { label: "Draws", href: "/dashboard/organizer/draws", icon: Gavel },
  { label: "Notifications", href: "/dashboard/organizer/notifications", icon: Bell },
  { label: "Settings", href: "/dashboard/organizer/settings", icon: Settings },
];

export function OrganizerDashboardContent() {
  const { data, isPending, isError, error, refetch } = useOrganizerDashboard();
  const memberDashboard = useMemberDashboard();

  if (isPending) {
    return (
      <PageContainer title="Organizer Dashboard">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {Array.from({ length: 5 }, (_, index) => (
            <Skeleton key={index} className="h-24 rounded-2xl" />
          ))}
        </div>
        <Skeleton className="h-64 rounded-2xl" />
      </PageContainer>
    );
  }

  if (isError) {
    return (
      <PageContainer title="Organizer Dashboard">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  const totalMembers = data.groups.reduce((sum, group) => sum + group.memberCount, 0);
  const upcomingDraws = data.groups
    .map((group) => group.nextDraw)
    .filter((draw): draw is NonNullable<typeof draw> => draw !== null)
    .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime());
  const nextUpcomingDraw = upcomingDraws[0] ?? null;

  return (
    <PageContainer title="Organizer Dashboard" description="Every group you organize, at a glance.">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <StatTile icon={Users} label="Current groups" value={data.groups.length} />
        <StatTile icon={Users} label="Total members" value={totalMembers} />
        <StatTile
          icon={Gavel}
          label="Upcoming draw"
          value={nextUpcomingDraw ? formatDateTime(nextUpcomingDraw.scheduledAt) : "—"}
          hint={nextUpcomingDraw ? undefined : "None scheduled"}
        />
        <StatTile icon={Wallet} label="Pending payments" value="" comingSoon />
        <StatTile icon={Users} label="Current cycle" value="" comingSoon />
      </div>

      {data.groups.length === 0 ? (
        <EmptyState
          icon={Users}
          title="You don't organize any groups yet"
          description="Groups you create or already own will appear here with their members, draws, and contribution progress."
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.groups.map((group) => (
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

      {memberDashboard.data && (
        <RecentNotificationsCard notifications={memberDashboard.data.recentNotifications} />
      )}

      <div className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold text-foreground">Quick actions</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
          {organizerQuickActions.map((action) => {
            const Icon = action.icon;
            return (
              <Link
                key={action.href}
                href={action.href}
                className={cn(
                  "group flex flex-col items-center gap-2 rounded-2xl border border-border/60 bg-card px-3 py-4 text-center transition-colors hover:border-primary/40 hover:bg-primary/5"
                )}
              >
                <span className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
                  <Icon className="size-4.5" />
                </span>
                <span className="text-xs font-medium text-foreground">{action.label}</span>
              </Link>
            );
          })}
        </div>
      </div>
    </PageContainer>
  );
}
