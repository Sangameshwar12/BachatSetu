"use client";

import { ArrowRight, Info, Wallet } from "lucide-react";
import Link from "next/link";

import { PageContainer } from "@/components/dashboard/page-container";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { useOrganizerDashboard } from "@/hooks/use-organizer-dashboard";

export function OrganizerPaymentsContent() {
  const { data, isPending, isError, error, refetch } = useOrganizerDashboard();

  if (isPending) {
    return (
      <PageContainer title="Payments">
        <Skeleton className="h-64 rounded-2xl" />
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
    <PageContainer title="Payments" description="Contribution progress across every group you organize.">
      {data.groups.length === 0 ? (
        <EmptyState
          icon={Wallet}
          title="No groups yet"
          description="Once you organize a group, its contribution progress will show up here."
        />
      ) : (
        <Card>
          <CardContent className="divide-y divide-border/60">
            {data.groups.map((group) => (
              <div key={group.groupId} className="flex items-center gap-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">{group.name}</p>
                  <div className="mt-1.5 flex items-center gap-2">
                    <Progress value={group.contributionProgressPercent} className="w-32" />
                    <span className="text-xs text-muted-foreground">
                      {group.contributionProgressPercent}% paid this cycle
                    </span>
                  </div>
                </div>
                <Link
                  href={`/dashboard/organizer/groups/${group.groupId}`}
                  className="inline-flex shrink-0 items-center gap-1 text-sm font-medium text-primary hover:underline"
                >
                  View <ArrowRight className="size-3.5" />
                </Link>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <Alert>
        <Info />
        <AlertTitle>A full payment ledger isn&apos;t available yet</AlertTitle>
        <AlertDescription>
          The Payments API doesn&apos;t yet support filtering by group, so pending, verified, and
          failed payments can&apos;t be listed individually per group — only the aggregate
          contribution progress above is available.
        </AlertDescription>
      </Alert>
    </PageContainer>
  );
}
