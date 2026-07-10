"use client";

import { Banknote, PiggyBank, Wallet } from "lucide-react";

import { PageContainer } from "@/components/dashboard/page-container";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { useAuth } from "@/contexts/auth-context";
import { CurrentGroupCard } from "@/features/dashboard/current-group-card";
import { NextDrawCard } from "@/features/dashboard/next-draw-card";
import { QuickActionsGrid } from "@/features/dashboard/quick-actions-grid";
import { RecentNotificationsCard } from "@/features/dashboard/recent-notifications-card";
import { StatTile } from "@/features/dashboard/stat-tile";
import { WelcomeEmptyState } from "@/features/dashboard/welcome-empty-state";
import { isNoActiveGroupError, useMemberDashboard } from "@/hooks/use-member-dashboard";
import { formatPaiseAsRupees } from "@/utils/format";

export function DashboardContent() {
  const { session } = useAuth();
  const { data, isPending, isError, error, refetch } = useMemberDashboard();

  if (isPending) {
    return (
      <PageContainer title="Dashboard">
        <div className="grid gap-4 sm:grid-cols-3">
          <Skeleton className="h-24 rounded-2xl" />
          <Skeleton className="h-24 rounded-2xl" />
          <Skeleton className="h-24 rounded-2xl" />
        </div>
        <Skeleton className="h-64 rounded-2xl" />
      </PageContainer>
    );
  }

  if (isError) {
    if (isNoActiveGroupError(error)) {
      return (
        <PageContainer title="Dashboard">
          <WelcomeEmptyState mobileNumber={session?.mobileNumber} />
        </PageContainer>
      );
    }
    return (
      <PageContainer title="Dashboard">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  return (
    <PageContainer title="Welcome back" description={session?.mobileNumber ?? undefined}>
      <div className="grid gap-4 sm:grid-cols-3">
        <StatTile
          icon={Wallet}
          label="This cycle's contribution"
          value={formatPaiseAsRupees(data.currentGroup.upcomingInstallmentAmountPaise)}
          hint={data.currentGroup.frequency}
        />
        <StatTile
          icon={Banknote}
          label="Latest payment status"
          value={data.latestPaymentStatus ? <StatusBadge status={data.latestPaymentStatus} /> : "—"}
          hint={data.latestPaymentStatus ? undefined : "No payments recorded yet"}
        />
        <StatTile icon={PiggyBank} label="Total savings" value="" comingSoon />
      </div>

      <div className="grid gap-4 lg:grid-cols-[2fr_1fr]">
        <CurrentGroupCard group={data.currentGroup} />
        <div className="flex flex-col gap-4">
          <NextDrawCard draw={data.nextDraw} />
          <RecentNotificationsCard notifications={data.recentNotifications} />
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold text-foreground">Quick actions</h2>
        <QuickActionsGrid />
      </div>
    </PageContainer>
  );
}
