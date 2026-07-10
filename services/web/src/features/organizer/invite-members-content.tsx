"use client";

import { Copy, Loader2, RefreshCw, Share2, Ticket, Trash2 } from "lucide-react";
import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
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
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageContainer } from "@/components/dashboard/page-container";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import {
  isNoActiveInvitationError,
  useCreateInvitation,
  useCurrentInvitation,
  useRevokeInvitation,
} from "@/hooks/use-group-invitation";
import { ApiError } from "@/services/api-client";
import { formatDateTime } from "@/utils/format";

export function InviteMembersContent({ groupId }: { groupId: string }) {
  const { data: invitation, isPending, isError, error, refetch } = useCurrentInvitation(groupId);
  const createInvitation = useCreateInvitation(groupId);
  const revokeInvitation = useRevokeInvitation(groupId);
  const [copied, setCopied] = useState<"code" | "link" | null>(null);

  if (isPending) {
    return (
      <PageContainer title="Invite members">
        <Skeleton className="h-80 rounded-2xl" />
      </PageContainer>
    );
  }

  if (isError && !isNoActiveInvitationError(error)) {
    return (
      <PageContainer title="Invite members">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  async function handleGenerate() {
    try {
      await createInvitation.mutateAsync({ type: "LINK" });
      toast.success("Invitation generated.");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't generate an invitation.");
    }
  }

  async function handleRevoke() {
    try {
      await revokeInvitation.mutateAsync();
      toast.success("Invitation revoked.");
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.message : "Couldn't revoke this invitation.");
    }
  }

  if (!invitation) {
    return (
      <PageContainer title="Invite members">
        <EmptyState
          icon={Ticket}
          title="No active invitation"
          description="Generate an invitation code, link, and QR code that people can use to join this group."
          action={
            <Button onClick={handleGenerate} disabled={createInvitation.isPending}>
              {createInvitation.isPending && <Loader2 className="size-4 animate-spin" />}
              Generate invitation
            </Button>
          }
        />
      </PageContainer>
    );
  }

  const fullJoinLink =
    typeof window !== "undefined" ? `${window.location.origin}${invitation.joinLink}` : invitation.joinLink;

  async function copyToClipboard(value: string, which: "code" | "link") {
    await navigator.clipboard.writeText(value);
    setCopied(which);
    toast.success("Copied to clipboard.");
    setTimeout(() => setCopied(null), 2000);
  }

  async function share() {
    if (navigator.share) {
      try {
        await navigator.share({ title: "Join my BachatSetu group", url: fullJoinLink });
      } catch {
        // User cancelled the share sheet — nothing to do.
      }
    } else {
      await copyToClipboard(fullJoinLink, "link");
    }
  }

  return (
    <PageContainer title="Invite members" description="Share this code, link, or QR to add members.">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Current invitation</CardTitle>
            <StatusBadge status={invitation.status} />
          </div>
        </CardHeader>
        <CardContent className="flex flex-col items-center gap-6">
          <div className="rounded-2xl border border-border/60 bg-white p-4">
            <QRCodeSVG value={fullJoinLink} size={176} />
          </div>

          <div className="flex w-full flex-col gap-3">
            <div className="flex items-center justify-between rounded-lg border border-border/60 px-3 py-2.5">
              <div>
                <p className="text-xs text-muted-foreground">Invite code</p>
                <p className="font-mono text-sm font-medium tracking-wide text-foreground">
                  {invitation.code}
                </p>
              </div>
              <Button
                variant="ghost"
                size="icon"
                aria-label="Copy invite code"
                onClick={() => copyToClipboard(invitation.code, "code")}
              >
                <Copy className={copied === "code" ? "size-4 text-success" : "size-4"} />
              </Button>
            </div>

            <div className="flex items-center justify-between gap-2 rounded-lg border border-border/60 px-3 py-2.5">
              <div className="min-w-0">
                <p className="text-xs text-muted-foreground">Invite link</p>
                <p className="truncate text-sm font-medium text-foreground">{fullJoinLink}</p>
              </div>
              <div className="flex shrink-0 gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Copy invite link"
                  onClick={() => copyToClipboard(fullJoinLink, "link")}
                >
                  <Copy className={copied === "link" ? "size-4 text-success" : "size-4"} />
                </Button>
                <Button variant="ghost" size="icon" aria-label="Share invite link" onClick={share}>
                  <Share2 className="size-4" />
                </Button>
              </div>
            </div>
          </div>

          <p className="text-xs text-muted-foreground">Expires {formatDateTime(invitation.expiresAt)}</p>

          <div className="flex w-full flex-col gap-2 sm:flex-row">
            <Button
              variant="outline"
              className="flex-1"
              onClick={handleGenerate}
              disabled={createInvitation.isPending}
            >
              {createInvitation.isPending && <Loader2 className="size-4 animate-spin" />}
              <RefreshCw className="size-4" /> Generate new
            </Button>

            <AlertDialog>
              <AlertDialogTrigger render={<Button variant="destructive" className="flex-1" />}>
                <Trash2 className="size-4" /> Revoke
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Revoke this invitation?</AlertDialogTitle>
                  <AlertDialogDescription>
                    The code, link, and QR code above will stop working immediately.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    className="bg-destructive text-white hover:bg-destructive/90"
                    onClick={handleRevoke}
                    disabled={revokeInvitation.isPending}
                  >
                    {revokeInvitation.isPending && <Loader2 className="size-4 animate-spin" />}
                    Revoke
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </CardContent>
      </Card>
    </PageContainer>
  );
}
