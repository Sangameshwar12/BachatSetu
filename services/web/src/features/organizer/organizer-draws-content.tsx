"use client";

import { ArrowRight, Gavel, Info } from "lucide-react";
import Link from "next/link";

import { PageContainer } from "@/components/dashboard/page-container";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { useOrganizerDashboard } from "@/hooks/use-organizer-dashboard";
import { formatDateTime } from "@/utils/format";

export function OrganizerDrawsContent() {
  const { data, isPending, isError, error, refetch } = useOrganizerDashboard();

  if (isPending) {
    return (
      <PageContainer title="Draws">
        <Skeleton className="h-64 rounded-2xl" />
      </PageContainer>
    );
  }

  if (isError) {
    return (
      <PageContainer title="Draws">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  const groupsWithDraws = data.groups.filter((group) => group.nextDraw !== null);

  return (
    <PageContainer title="Draws" description="Upcoming draws across every group you organize.">
      {groupsWithDraws.length === 0 ? (
        <EmptyState
          icon={Gavel}
          title="No upcoming draws"
          description="Once a draw is scheduled for one of your groups, it will appear here."
        />
      ) : (
        <Card>
          <CardContent className="divide-y divide-border/60">
            {groupsWithDraws.map((group) => (
              <div key={group.groupId} className="flex items-center gap-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{group.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {formatDateTime(group.nextDraw!.scheduledAt)}
                  </p>
                </div>
                <StatusBadge status={group.nextDraw!.status} />
                <Link
                  href={`/dashboard/organizer/groups/${group.groupId}`}
                  className="inline-flex shrink-0 items-center gap-1 text-sm font-medium text-primary hover:underline"
                >
                  Manage <ArrowRight className="size-3.5" />
                </Link>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <Alert>
        <Info />
        <AlertTitle>Full draw history isn&apos;t available yet</AlertTitle>
        <AlertDescription>
          The Draws API doesn&apos;t yet support filtering by group, so completed draws and past
          winners can&apos;t be listed here — only each group&apos;s single next scheduled draw is
          available. Conduct and close a draw from its group&apos;s Draws tab.
        </AlertDescription>
      </Alert>
    </PageContainer>
  );
}
