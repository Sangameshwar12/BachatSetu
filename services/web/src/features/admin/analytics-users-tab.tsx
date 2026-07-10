import { Building2, Users } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { AnalyticsTrendChart } from "@/features/admin/analytics-trend-chart";
import { DistributionList } from "@/features/admin/distribution-list";
import { StatTile } from "@/features/dashboard/stat-tile";
import { useUserAnalytics } from "@/hooks/use-admin-analytics";

export function AnalyticsUsersTab() {
  const { data, isPending, isError, error, refetch } = useUserAnalytics();

  if (isPending) {
    return (
      <div className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-3">
          {Array.from({ length: 3 }, (_, index) => (
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

  const trend = data.monthlyRegistrations.map((point) => ({ label: point.month, value: point.count }));

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-3">
        <StatTile icon={Users} label="Total users" value={data.totalUsers} />
        <StatTile icon={Users} label="Active users" value={data.activeUsers} />
        <StatTile icon={Users} label="Disabled users" value={data.disabledUsers} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>New registrations per month</CardTitle>
        </CardHeader>
        <CardContent>
          {trend.length === 0 ? (
            <p className="text-sm text-muted-foreground">No monthly data yet.</p>
          ) : (
            <AnalyticsTrendChart data={trend} kind="bar" />
          )}
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Preferred language</CardTitle>
          </CardHeader>
          <CardContent>
            <DistributionList entries={data.preferredLanguageDistribution} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>
              <span className="flex items-center gap-2">
                <Building2 className="size-4" /> Users per tenant
              </span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <DistributionList
              entries={data.usersPerTenant.map((row) => ({ key: row.tenantId, count: row.userCount }))}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
