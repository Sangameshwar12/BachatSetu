"use client";

import { CalendarClock, Loader2 } from "lucide-react";
import { toast } from "sonner";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { useCollectionSummary, useMarkMemberPaid } from "@/hooks/use-collection";
import { ApiError } from "@/services/api-client";
import { formatDate, formatPaiseAsRupees } from "@/utils/format";

function shortId(memberId: string): string {
  return `Member ${memberId.slice(0, 8)}`;
}

export function OrganizerGroupPaymentsTab({ groupId, isOwner }: { groupId: string; isOwner: boolean }) {
  const { data, isPending, isError, error, refetch } = useCollectionSummary(groupId);
  const markPaid = useMarkMemberPaid(groupId);

  if (isPending) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-40 rounded-2xl" />
        <Skeleton className="h-64 rounded-2xl" />
      </div>
    );
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  if (!data.cycleActive) {
    return (
      <EmptyState
        icon={CalendarClock}
        title="No active contribution cycle"
        description="Collection starts once the group is activated, and stops once every scheduled cycle has completed."
      />
    );
  }

  async function handleMarkPaid(memberId: string) {
    try {
      await markPaid.mutateAsync(memberId);
      toast.success("Payment recorded.");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't record this payment.");
    }
  }

  const collectedPercent = data.totalExpectedPaise > 0
    ? Math.round((data.totalCollectedPaise / data.totalExpectedPaise) * 100)
    : 0;

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Cycle {data.cycleNumber}</CardTitle>
            <span className="text-xs text-muted-foreground">
              Due {data.dueDate && formatDate(data.dueDate)}
            </span>
          </div>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div>
              <p className="text-xs text-muted-foreground">Expected</p>
              <p className="text-sm font-medium text-foreground">
                {formatPaiseAsRupees(data.totalExpectedPaise)}
              </p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Collected</p>
              <p className="text-sm font-medium text-foreground">
                {formatPaiseAsRupees(data.totalCollectedPaise)}
              </p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Remaining</p>
              <p className="text-sm font-medium text-foreground">
                {formatPaiseAsRupees(data.totalRemainingPaise)}
              </p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Paid / Pending / Overdue</p>
              <p className="text-sm font-medium text-foreground">
                {data.paidCount} / {data.pendingCount} / {data.overdueCount}
              </p>
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Collection progress</span>
              <span className="font-medium text-foreground">{collectedPercent}%</span>
            </div>
            <Progress value={collectedPercent} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="divide-y divide-border/60 p-0">
          {data.members.map((member) => {
            const canMarkPaid = isOwner && member.status !== "PAID";
            return (
              <div key={member.memberId} className="flex items-center justify-between gap-4 px-4 py-3">
                <div className="flex flex-col gap-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium text-foreground">{shortId(member.memberId)}</span>
                    <StatusBadge status={member.status} />
                  </div>
                  <span className="text-xs text-muted-foreground">
                    {formatPaiseAsRupees(
                      member.status === "PAID" ? member.collectedAmountPaise : member.expectedAmountPaise
                    )}
                    {member.paidAt
                      ? ` · Paid ${formatDate(member.paidAt)}`
                      : ` · Due ${formatDate(member.dueDate)}`}
                  </span>
                </div>

                {canMarkPaid && (
                  <AlertDialog>
                    <AlertDialogTrigger
                      render={<Button variant="outline" size="sm" disabled={markPaid.isPending} />}
                    >
                      {markPaid.isPending && markPaid.variables === member.memberId ? (
                        <Loader2 className="size-4 animate-spin" />
                      ) : (
                        "Mark paid"
                      )}
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Record this member as paid?</AlertDialogTitle>
                        <AlertDialogDescription>
                          This records {formatPaiseAsRupees(member.expectedAmountPaise)} as collected in cash
                          for the current cycle. This can&apos;t be undone from here.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={() => handleMarkPaid(member.memberId)}>
                          Mark paid
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                )}
              </div>
            );
          })}
          {data.members.length === 0 && (
            <div className="px-4 py-3">
              <Badge variant="outline">No members yet</Badge>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
