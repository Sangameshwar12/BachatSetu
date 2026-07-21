"use client";

import { CalendarClock, Info, Wallet } from "lucide-react";

import { PageContainer } from "@/components/dashboard/page-container";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { useAuth } from "@/contexts/auth-context";
import { useCollectionSummary } from "@/hooks/use-collection";
import { isNoActiveGroupError, useMemberDashboard } from "@/hooks/use-member-dashboard";
import { formatDate, formatPaiseAsRupees } from "@/utils/format";

export function PaymentsContent() {
  const { session } = useAuth();
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
    <PageContainer title="My contributions" description="Your contribution status for your group.">
      <MyCurrentCycleContribution groupId={data.currentGroup.groupId} memberId={session?.userId} />

      <Alert>
        <Info />
        <AlertTitle>Full contribution history isn&apos;t available yet</AlertTitle>
        <AlertDescription>
          Only your current cycle&apos;s status is shown today — a complete history across every
          past cycle, and a linked receipt once paid, will land once those endpoints ship.
        </AlertDescription>
      </Alert>
    </PageContainer>
  );
}

function MyCurrentCycleContribution({ groupId, memberId }: { groupId: string; memberId: string | undefined }) {
  const { data, isPending, isError, error, refetch } = useCollectionSummary(groupId);

  if (isPending) {
    return <Skeleton className="h-40 rounded-2xl" />;
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  if (!data.cycleActive) {
    return (
      <EmptyState
        icon={CalendarClock}
        title="No active contribution cycle"
        description="Your group hasn't started collecting yet, or every scheduled cycle has completed."
      />
    );
  }

  const myContribution = data.members.find((member) => member.memberId === memberId);

  if (!myContribution) {
    return (
      <EmptyState
        icon={Wallet}
        title="No contribution recorded yet"
        description="Once this cycle's contribution is recorded, its status will appear here."
      />
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-base">Cycle {data.cycleNumber}</CardTitle>
          <StatusBadge status={myContribution.status} />
        </div>
      </CardHeader>
      <CardContent className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <div>
          <p className="text-xs text-muted-foreground">Amount</p>
          <p className="text-sm font-medium text-foreground">
            {formatPaiseAsRupees(myContribution.expectedAmountPaise)}
          </p>
        </div>
        <div>
          <p className="text-xs text-muted-foreground">Due date</p>
          <p className="text-sm font-medium text-foreground">{formatDate(myContribution.dueDate)}</p>
        </div>
        {myContribution.paidAt && (
          <div>
            <p className="text-xs text-muted-foreground">Paid on</p>
            <p className="text-sm font-medium text-foreground">{formatDate(myContribution.paidAt)}</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
