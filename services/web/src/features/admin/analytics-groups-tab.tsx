import { Gavel, Percent, Users } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { AnalyticsTrendChart } from "@/features/admin/analytics-trend-chart-lazy";
import { StatTile } from "@/features/dashboard/stat-tile";
import { useGroupAnalytics } from "@/hooks/use-admin-analytics";
import { formatPaiseAsRupees } from "@/utils/format";

export function AnalyticsGroupsTab() {
  const { data, isPending, isError, error, refetch } = useGroupAnalytics();

  if (isPending) {
    return (
      <div className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }, (_, index) => (
            <Skeleton key={index} className="h-24 rounded-2xl" />
          ))}
        </div>
        <Skeleton className="h-64 rounded-2xl" />
      </div>
    );
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  const trend = data.monthlyNewGroups.map((point) => ({ label: point.month, value: point.count }));

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile
          icon={Gavel}
          label="Groups (active / completed)"
          value={`${data.activeGroups} / ${data.completedGroups}`}
          hint={`${data.totalGroups} total`}
        />
        <StatTile icon={Users} label="Avg members per group" value={data.averageMembersPerGroup.toFixed(1)} />
        <StatTile
          icon={Percent}
          label="Avg contribution"
          value={formatPaiseAsRupees(Math.round(data.averageContributionAmountPaise))}
        />
        <StatTile icon={Percent} label="Draw completion rate" value={`${Math.round(data.drawCompletionRate * 100)}%`} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>New groups per month</CardTitle>
        </CardHeader>
        <CardContent>
          {trend.length === 0 ? (
            <p className="text-sm text-muted-foreground">No monthly data yet.</p>
          ) : (
            <AnalyticsTrendChart data={trend} kind="bar" />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
