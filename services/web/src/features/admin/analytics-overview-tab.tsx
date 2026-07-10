import { Banknote, Bell, FileText, Gavel, Receipt, Users } from "lucide-react";

import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { StatTile } from "@/features/dashboard/stat-tile";
import { useOverviewAnalytics } from "@/hooks/use-admin-analytics";
import { formatCompactNumber } from "@/utils/format";

export function AnalyticsOverviewTab() {
  const { data, isPending, isError, error, refetch } = useOverviewAnalytics();

  if (isPending) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 8 }, (_, index) => (
          <Skeleton key={index} className="h-24 rounded-2xl" />
        ))}
      </div>
    );
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <StatTile icon={Users} label="Total users" value={formatCompactNumber(data.totalUsers)} />
      <StatTile
        icon={Users}
        label="Active / inactive"
        value={`${formatCompactNumber(data.activeUsers)} / ${formatCompactNumber(data.inactiveUsers)}`}
      />
      <StatTile icon={Gavel} label="Total tenants" value={formatCompactNumber(data.totalTenants)} />
      <StatTile
        icon={Gavel}
        label="Groups (active / completed)"
        value={`${formatCompactNumber(data.activeGroups)} / ${formatCompactNumber(data.completedGroups)}`}
        hint={`${formatCompactNumber(data.totalGroups)} total`}
      />
      <StatTile
        icon={Banknote}
        label="Payments (verified / failed)"
        value={`${formatCompactNumber(data.verifiedPayments)} / ${formatCompactNumber(data.failedPayments)}`}
        hint={`${formatCompactNumber(data.totalPayments)} total`}
      />
      <StatTile icon={Receipt} label="Total receipts" value={formatCompactNumber(data.totalReceipts)} />
      <StatTile icon={Bell} label="Total notifications" value={formatCompactNumber(data.totalNotifications)} />
      <StatTile icon={FileText} label="Total stored files" value={formatCompactNumber(data.totalStoredFiles)} />
    </div>
  );
}
