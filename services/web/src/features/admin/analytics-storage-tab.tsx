import { FileText, HardDrive } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { AnalyticsTrendChart } from "@/features/admin/analytics-trend-chart";
import { DistributionList } from "@/features/admin/distribution-list";
import { StatTile } from "@/features/dashboard/stat-tile";
import { useStorageAnalytics } from "@/hooks/use-admin-analytics";
import { formatBytes, formatDate } from "@/utils/format";

export function AnalyticsStorageTab() {
  const { data, isPending, isError, error, refetch } = useStorageAnalytics();

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

  const trend = data.uploadsPerDay.map((point) => ({ label: formatDate(point.date), value: point.count }));

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-3">
        <StatTile icon={FileText} label="Total files" value={data.totalFiles} />
        <StatTile icon={HardDrive} label="Total storage used" value={formatBytes(data.totalStorageBytes)} />
        <StatTile icon={HardDrive} label="Average file size" value={formatBytes(data.averageFileSizeBytes)} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Uploads — last 30 days</CardTitle>
        </CardHeader>
        <CardContent>
          {trend.length === 0 ? (
            <p className="text-sm text-muted-foreground">No uploads in the trailing 30 days.</p>
          ) : (
            <AnalyticsTrendChart data={trend} />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Storage provider</CardTitle>
        </CardHeader>
        <CardContent>
          <DistributionList entries={data.storageProviderDistribution} />
        </CardContent>
      </Card>
    </div>
  );
}
