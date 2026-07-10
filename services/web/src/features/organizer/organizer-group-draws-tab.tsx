"use client";

import { Gavel, Info, Loader2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { ErrorState } from "@/components/shared/error-state";
import { GroupTabPlaceholder } from "@/features/groups/group-tab-placeholder";
import { useCloseDraw, useConductDraw, useDraw } from "@/hooks/use-draw";
import { ApiError } from "@/services/api-client";
import type { OrganizerGroupResponse } from "@/types/organizer-dashboard";
import { formatDateTime } from "@/utils/format";

export function OrganizerGroupDrawsTab({ group }: { group: OrganizerGroupResponse | undefined }) {
  const drawId = group?.nextDraw?.drawId ?? null;
  const { data: draw, isPending, isError, error, refetch } = useDraw(drawId);
  const conductDraw = useConductDraw(drawId ?? "");
  const closeDraw = useCloseDraw(drawId ?? "");
  const [winnerId, setWinnerId] = useState("");

  if (!drawId) {
    return (
      <div className="flex flex-col gap-4">
        <GroupTabPlaceholder
          icon={Gavel}
          title="No upcoming draw for this group"
          description="Once a draw is scheduled for this group's current cycle, it will appear here."
        />
        <Alert>
          <Info />
          <AlertTitle>Full draw history isn&apos;t available yet</AlertTitle>
          <AlertDescription>
            The Draws API doesn&apos;t yet support filtering by group, so past winners and draw
            dates for this group can&apos;t be listed here — only the single next scheduled draw
            is available.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (isPending) {
    return <Card className="h-48 animate-pulse" />;
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  async function handleConduct() {
    try {
      await conductDraw.mutateAsync();
      toast.success("Draw conducted.");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't conduct this draw.");
    }
  }

  async function handleClose() {
    try {
      await closeDraw.mutateAsync({ winnerId: winnerId.trim() });
      toast.success("Draw closed.");
      setWinnerId("");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't close this draw.");
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Draw #{draw.drawNumber}</CardTitle>
            <StatusBadge status={draw.status} />
          </div>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <p className="text-xs text-muted-foreground">Type</p>
              <p className="font-medium text-foreground">{draw.type}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Scheduled</p>
              <p className="font-medium text-foreground">{formatDateTime(draw.scheduledAt)}</p>
            </div>
            {draw.winnerMemberId && (
              <div>
                <p className="text-xs text-muted-foreground">Winner (member ID)</p>
                <p className="font-medium text-foreground">{draw.winnerMemberId}</p>
              </div>
            )}
          </div>

          {draw.status === "SCHEDULED" && (
            <Button onClick={handleConduct} disabled={conductDraw.isPending}>
              {conductDraw.isPending && <Loader2 className="size-4 animate-spin" />}
              Conduct draw
            </Button>
          )}

          {draw.status === "OPEN" && (
            <div className="flex flex-col gap-2 rounded-lg border border-border/60 p-3">
              <Label htmlFor="winnerId">Winning member ID</Label>
              <Input
                id="winnerId"
                placeholder="123e4567-e89b-12d3-a456-426614174000"
                value={winnerId}
                onChange={(event) => setWinnerId(event.target.value)}
              />
              <p className="text-xs text-muted-foreground">
                There&apos;s no member picker yet (no group-scoped member list endpoint) — enter
                the winning member&apos;s ID directly.
              </p>
              <Button onClick={handleClose} disabled={!winnerId.trim() || closeDraw.isPending}>
                {closeDraw.isPending && <Loader2 className="size-4 animate-spin" />}
                Close draw with this winner
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <Alert>
        <Info />
        <AlertTitle>Full draw history isn&apos;t available yet</AlertTitle>
        <AlertDescription>
          Only this group&apos;s single next scheduled draw is available — the Draws API doesn&apos;t
          yet support filtering by group for a complete history.
        </AlertDescription>
      </Alert>
    </div>
  );
}
