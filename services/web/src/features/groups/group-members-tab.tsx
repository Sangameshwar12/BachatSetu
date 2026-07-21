"use client";

import { Loader2, Trash2, Users } from "lucide-react";

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
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import type { SavingsGroupResponse } from "@/types/group";
import { formatDate } from "@/utils/format";

interface GroupMembersTabProps {
  group: SavingsGroupResponse;
  currentUserId?: string;
  /** Organizer-only: when provided, active non-owner rows get a Remove action. */
  onRemove?: (memberId: string) => void;
  removingMemberId?: string;
}

function shortId(memberId: string): string {
  return `Member ${memberId.slice(0, 8)}`;
}

export function GroupMembersTab({ group, currentUserId, onRemove, removingMemberId }: GroupMembersTabProps) {
  if (group.members.length === 0) {
    return (
      <EmptyState
        icon={Users}
        title="No members yet"
        description="Members will appear here once they join this group."
      />
    );
  }

  const members = [...group.members].sort((a, b) => {
    if (a.memberId === group.ownerId) return -1;
    if (b.memberId === group.ownerId) return 1;
    return new Date(a.joinedAt).getTime() - new Date(b.joinedAt).getTime();
  });

  return (
    <Card>
      <CardContent className="divide-y divide-border/60 p-0">
        {members.map((member) => {
          const isOwner = member.memberId === group.ownerId;
          const isYou = member.memberId === currentUserId;
          const canRemove = Boolean(onRemove) && !isOwner && member.active;
          return (
            <div key={member.memberId} className="flex items-center justify-between gap-4 px-4 py-3">
              <div className="flex flex-col gap-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm font-medium text-foreground">
                    {isOwner ? group.organizerName ?? "Group organizer" : shortId(member.memberId)}
                  </span>
                  {isOwner && (
                    <Badge variant="outline" className="border-transparent bg-info/10 text-info">
                      Owner
                    </Badge>
                  )}
                  {isYou && <Badge variant="outline">You</Badge>}
                  {!member.active && (
                    <Badge variant="outline" className="text-muted-foreground">
                      Removed
                    </Badge>
                  )}
                </div>
                <span className="text-xs text-muted-foreground">Joined {formatDate(member.joinedAt)}</span>
              </div>

              {canRemove && (
                <AlertDialog>
                  <AlertDialogTrigger
                    render={
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label="Remove member"
                        disabled={removingMemberId === member.memberId}
                      />
                    }
                  >
                    {removingMemberId === member.memberId ? (
                      <Loader2 className="size-4 animate-spin" />
                    ) : (
                      <Trash2 className="size-4 text-destructive" />
                    )}
                  </AlertDialogTrigger>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Remove this member?</AlertDialogTitle>
                      <AlertDialogDescription>
                        They will lose access to this group and will need a new invitation to rejoin.
                      </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Cancel</AlertDialogCancel>
                      <AlertDialogAction
                        className="bg-destructive text-white hover:bg-destructive/90"
                        onClick={() => onRemove?.(member.memberId)}
                      >
                        Remove
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              )}
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
