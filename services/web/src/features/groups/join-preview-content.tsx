"use client";

import { ArrowRight, Loader2, Users } from "lucide-react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { StatusBadge } from "@/components/dashboard/status-badge";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/auth-context";
import { AuthShell } from "@/features/auth/auth-shell";
import { useInvitationPreview, useJoinGroup } from "@/hooks/use-invitation";
import { ApiError } from "@/services/api-client";
import { formatPaiseAsRupees } from "@/utils/format";

export function JoinPreviewContent({ token }: { token: string }) {
  const router = useRouter();
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const preview = useInvitationPreview(token);
  const joinGroup = useJoinGroup();

  async function handleJoin() {
    if (!isAuthenticated) {
      router.push(`/login?redirect=${encodeURIComponent(`/join/${token}`)}`);
      return;
    }
    try {
      const result = await joinGroup.mutateAsync({ token, channel: "LINK" });
      toast.success("Joined successfully");
      router.push(`/dashboard/groups/${result.groupId}`);
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't join that group — try again.");
    }
  }

  if (preview.isPending || isAuthLoading) {
    return (
      <AuthShell title="Join a group">
        <div className="flex justify-center py-6">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      </AuthShell>
    );
  }

  if (preview.isError) {
    return (
      <AuthShell title="Join a group">
        <p className="text-sm text-destructive">
          {preview.error instanceof ApiError
            ? preview.error.message
            : "This invitation link doesn't look right."}
        </p>
      </AuthShell>
    );
  }

  const invite = preview.data;

  return (
    <AuthShell title={invite.groupName} description="You've been invited to join this savings group.">
      <div className="flex flex-col gap-5">
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <p className="text-xs text-muted-foreground">Organizer</p>
            <p className="font-medium text-foreground">{invite.organizerName}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Monthly amount</p>
            <p className="font-medium text-foreground">
              {formatPaiseAsRupees(invite.contributionAmountPaise)}
            </p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Frequency</p>
            <p className="font-medium text-foreground">{invite.frequency}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Members</p>
            <p className="inline-flex items-center gap-1 font-medium text-foreground">
              <Users className="size-3.5" />
              {invite.memberCount}/{invite.maximumMembers}
            </p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Status</p>
            <StatusBadge status={invite.status} />
          </div>
        </div>

        <Button className="w-full" disabled={joinGroup.isPending} onClick={handleJoin}>
          {joinGroup.isPending && <Loader2 className="size-4 animate-spin" />}
          {isAuthenticated ? "Join this group" : "Log in to join"} <ArrowRight className="size-4" />
        </Button>
      </div>
    </AuthShell>
  );
}
