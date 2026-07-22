"use client";

import {
  Banknote,
  BarChart3,
  Bell,
  Building2,
  Gavel,
  HardDrive,
  LifeBuoy,
  Receipt,
  Settings,
  Shield,
  Users,
} from "lucide-react";
import Link from "next/link";

import { PageContainer } from "@/components/dashboard/page-container";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { StatTile } from "@/features/dashboard/stat-tile";
import { useAdminStatistics } from "@/hooks/use-admin-statistics";
import { usePlatformOverview } from "@/hooks/use-platform-overview";
import { useStorageAnalytics } from "@/hooks/use-admin-analytics";
import { cn } from "@/lib/utils";
import { formatBytes, formatCompactNumber, formatPaiseAsRupees } from "@/utils/format";

const adminQuickActions = [
  { label: "Analytics", href: "/dashboard/admin/analytics", icon: BarChart3 },
  { label: "Users", href: "/dashboard/admin/users", icon: Users },
  { label: "Groups", href: "/dashboard/admin/groups", icon: Gavel },
  { label: "Tenants", href: "/dashboard/admin/tenants", icon: Building2 },
  { label: "Configuration", href: "/dashboard/admin/configuration", icon: Settings },
  { label: "Monitoring", href: "/dashboard/admin/monitoring", icon: Shield },
  { label: "Support", href: "/dashboard/admin/support", icon: LifeBuoy },
];

export function AdminDashboardContent() {
  const overview = usePlatformOverview();
  const statistics = useAdminStatistics();
  const storage = useStorageAnalytics();

  if (overview.isPending) {
    return (
      <PageContainer title="Platform Dashboard">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 8 }, (_, index) => (
            <Skeleton key={index} className="h-24 rounded-2xl" />
          ))}
        </div>
      </PageContainer>
    );
  }

  if (overview.isError) {
    return (
      <PageContainer title="Platform Dashboard">
        <ErrorState error={overview.error} onRetry={() => overview.refetch()} />
      </PageContainer>
    );
  }

  const data = overview.data;

  return (
    <PageContainer title="Platform Dashboard" description="Platform-wide totals, computed on demand.">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile icon={Users} label="Total users" value={formatCompactNumber(data.totalUsers)} />
        <StatTile icon={Gavel} label="Total groups" value={formatCompactNumber(data.totalGroups)} />
        <StatTile icon={Users} label="Total members" value={formatCompactNumber(data.totalMembers)} />
        <StatTile icon={Banknote} label="Total payments" value={formatCompactNumber(data.totalPayments)} />
        <StatTile icon={Receipt} label="Total receipts" value={formatCompactNumber(data.totalReceipts)} />
        <StatTile icon={Bell} label="Total notifications" value={formatCompactNumber(data.totalNotifications)} />
        {storage.data ? (
          <StatTile
            icon={HardDrive}
            label="Storage usage"
            value={formatBytes(storage.data.totalStorageBytes)}
            hint={`${formatCompactNumber(storage.data.totalFiles)} files`}
          />
        ) : storage.isError ? (
          <StatTile icon={HardDrive} label="Storage usage" value="" comingSoon />
        ) : (
          <Skeleton className="h-24 rounded-2xl" />
        )}
        <StatTile icon={Building2} label="Active tenants" value={formatCompactNumber(data.totalActiveTenants)} />
      </div>

      {statistics.data && statistics.data.totalUsers !== data.totalUsers && (
        <p className="text-xs text-muted-foreground">
          Note: the legacy statistics endpoint reports a slightly different active/disabled breakdown; the
          totals above come from the newer overview endpoint, which also includes today&apos;s activity.
        </p>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Recent activity (today)</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
            <div>
              <p className="text-xs text-muted-foreground">Signups</p>
              <p className="text-lg font-semibold text-foreground">{data.todaySignups}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Payments</p>
              <p className="text-lg font-semibold text-foreground">{data.todayPayments}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">New groups</p>
              <p className="text-lg font-semibold text-foreground">{data.todayGroups}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Notifications</p>
              <p className="text-lg font-semibold text-foreground">{data.todayNotifications}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Uploads</p>
              <p className="text-lg font-semibold text-foreground">{data.todayStorageUploads}</p>
            </div>
          </div>
          <p className="mt-4 text-xs text-muted-foreground">
            Total revenue to date: {formatPaiseAsRupees(data.totalRevenuePaise)}
          </p>
        </CardContent>
      </Card>

      <div className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold text-foreground">Quick actions</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {adminQuickActions.map((action) => {
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
