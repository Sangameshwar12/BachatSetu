"use client";

import { Info, Wallet } from "lucide-react";

import { PageContainer } from "@/components/dashboard/page-container";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { isNoActiveGroupError, useMemberDashboard } from "@/hooks/use-member-dashboard";

export function PaymentsContent() {
  const { data, isPending, isError, error, refetch } = useMemberDashboard();

  if (isPending) {
    return (
      <PageContainer title="Payments">
        <Skeleton className="h-24 rounded-2xl" />
        <Skeleton className="h-64 rounded-2xl" />
      </PageContainer>
    );
  }

  if (isNoActiveGroupError(error)) {
    return (
      <PageContainer title="Payments">
        <EmptyState
          icon={Wallet}
          title="No payments yet"
          description="Join a savings group to start making contributions — they'll show up here."
        />
      </PageContainer>
    );
  }

  if (isError) {
    return (
      <PageContainer title="Payments">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  return (
    <PageContainer title="Payments" description="Your contribution status for your group.">
      {data.latestPaymentStatus ? (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Latest payment</CardTitle>
          </CardHeader>
          <CardContent>
            <StatusBadge status={data.latestPaymentStatus} />
          </CardContent>
        </Card>
      ) : (
        <EmptyState
          icon={Wallet}
          title="No payments recorded yet"
          description="Once you make your first contribution to this group, its status will appear here."
        />
      )}

      <Alert>
        <Info />
        <AlertTitle>Full payment history isn&apos;t available yet</AlertTitle>
        <AlertDescription>
          The Payments API doesn&apos;t yet expose a per-member payment history — only your
          latest status is available today. A complete history with search and filters will land
          once that endpoint ships.
        </AlertDescription>
      </Alert>
    </PageContainer>
  );
}
