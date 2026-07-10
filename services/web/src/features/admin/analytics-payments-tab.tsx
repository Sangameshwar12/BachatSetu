import { Banknote, Percent, TrendingDown, TrendingUp } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { AnalyticsTrendChart } from "@/features/admin/analytics-trend-chart";
import { StatTile } from "@/features/dashboard/stat-tile";
import { usePaymentAnalytics } from "@/hooks/use-admin-analytics";
import { formatDate, formatPaiseAsRupees } from "@/utils/format";

export function AnalyticsPaymentsTab() {
  const { data, isPending, isError, error, refetch } = usePaymentAnalytics();

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

  const trend = data.paymentTrend.map((point) => ({ label: formatDate(point.date), value: point.count }));

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile
          icon={Banknote}
          label="Total volume"
          value={formatPaiseAsRupees(data.totalPaymentVolumePaise)}
          hint={`${formatPaiseAsRupees(data.verifiedPaymentVolumePaise)} verified`}
        />
        <StatTile
          icon={Banknote}
          label="Average contribution"
          value={formatPaiseAsRupees(Math.round(data.averageContributionPaise))}
        />
        <StatTile icon={TrendingUp} label="Success rate" value={`${Math.round(data.paymentSuccessRate * 100)}%`} />
        <StatTile icon={TrendingDown} label="Failure rate" value={`${Math.round(data.paymentFailureRate * 100)}%`} />
        <StatTile icon={Percent} label="Pending payments" value={data.pendingPaymentCount} />
        <StatTile icon={Percent} label="Failed payments" value={data.failedPaymentCount} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Payment activity — last 30 days</CardTitle>
        </CardHeader>
        <CardContent>
          {trend.length === 0 ? (
            <p className="text-sm text-muted-foreground">No payment activity in the trailing 30 days.</p>
          ) : (
            <AnalyticsTrendChart data={trend} kind="bar" />
          )}
        </CardContent>
      </Card>

      {trend.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Daily trend</CardTitle>
          </CardHeader>
          <CardContent className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="text-xs text-muted-foreground">
                  <th className="pb-2 font-medium">Date</th>
                  <th className="pb-2 font-medium">Payments</th>
                  <th className="pb-2 font-medium">Volume</th>
                </tr>
              </thead>
              <tbody>
                {data.paymentTrend.map((point) => (
                  <tr key={point.date} className="border-t border-border/60">
                    <td className="py-1.5">{formatDate(point.date)}</td>
                    <td className="py-1.5">{point.count}</td>
                    <td className="py-1.5">{formatPaiseAsRupees(point.volumePaise)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
