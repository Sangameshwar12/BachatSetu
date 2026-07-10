import { Bell, BellRing } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { DistributionList } from "@/features/admin/distribution-list";
import { StatTile } from "@/features/dashboard/stat-tile";
import { useNotificationAnalytics } from "@/hooks/use-admin-analytics";

export function AnalyticsNotificationsTab() {
  const { data, isPending, isError, error, refetch } = useNotificationAnalytics();

  if (isPending) {
    return (
      <div className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          {Array.from({ length: 2 }, (_, index) => (
            <Skeleton key={index} className="h-24 rounded-2xl" />
          ))}
        </div>
        <Skeleton className="h-48 rounded-2xl" />
      </div>
    );
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <StatTile icon={Bell} label="Total notifications" value={data.totalNotifications} />
        <StatTile icon={BellRing} label="Not yet delivered" value={data.unreadNotifications} />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Delivery status</CardTitle>
          </CardHeader>
          <CardContent>
            <DistributionList entries={data.deliveryStatusCounts} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Notification type</CardTitle>
          </CardHeader>
          <CardContent>
            <DistributionList entries={data.notificationTypeDistribution} />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
