"use client";

import { Info, Loader2, Play, Ban, XCircle } from "lucide-react";
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
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { useGroupLifecycle } from "@/hooks/use-group-lifecycle";
import { ApiError } from "@/services/api-client";
import type { SavingsGroupResponse } from "@/types/group";

export function OrganizerGroupSettingsTab({ group }: { group: SavingsGroupResponse }) {
  const { activate, suspend, close } = useGroupLifecycle(group.groupId);

  async function run(action: "activate" | "suspend" | "close") {
    try {
      if (action === "activate") await activate.mutateAsync();
      if (action === "suspend") await suspend.mutateAsync();
      if (action === "close") await close.mutateAsync();
      toast.success(`Group ${action}d.`);
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : `Couldn't ${action} this group.`);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Group lifecycle</CardTitle>
            <StatusBadge status={group.status} />
          </div>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          {group.status !== "ACTIVE" && group.status !== "CLOSED" && (
            <Button onClick={() => run("activate")} disabled={activate.isPending}>
              {activate.isPending && <Loader2 className="size-4 animate-spin" />}
              <Play className="size-4" /> Activate
            </Button>
          )}
          {group.status === "ACTIVE" && (
            <Button variant="outline" onClick={() => run("suspend")} disabled={suspend.isPending}>
              {suspend.isPending && <Loader2 className="size-4 animate-spin" />}
              <Ban className="size-4" /> Suspend
            </Button>
          )}
          {group.status !== "CLOSED" && (
            <AlertDialog>
              <AlertDialogTrigger render={<Button variant="destructive" />}>
                <XCircle className="size-4" /> Close group
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Close this group permanently?</AlertDialogTitle>
                  <AlertDialogDescription>
                    This cannot be undone. Members will no longer be able to contribute, and no
                    further draws can be scheduled.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    className="bg-destructive text-white hover:bg-destructive/90"
                    onClick={() => run("close")}
                    disabled={close.isPending}
                  >
                    {close.isPending && <Loader2 className="size-4 animate-spin" />}
                    Close group
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          )}
        </CardContent>
      </Card>

      <Alert>
        <Info />
        <AlertTitle>Contribution amount and rules can&apos;t be edited yet</AlertTitle>
        <AlertDescription>
          There&apos;s no backend endpoint to update a group&apos;s contribution amount, schedule,
          or payout rules after creation — those are set once, at creation time, only.
        </AlertDescription>
      </Alert>
    </div>
  );
}
