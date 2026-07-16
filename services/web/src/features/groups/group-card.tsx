import { ArrowRight, CalendarClock, MessageCircle, UserPlus, Users } from "lucide-react";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { StatusBadge } from "@/components/dashboard/status-badge";
import { cn } from "@/lib/utils";
import { formatDateTime, formatPaiseAsRupees } from "@/utils/format";

interface GroupCardProps {
  groupId: string;
  name: string;
  groupCode: string;
  /** Omitted for organizer summaries, which don't carry contribution amount/currency. */
  contributionAmountPaise?: number;
  currencyCode?: string;
  frequency?: string;
  memberCount: number;
  maximumMembers: number;
  status?: string;
  nextDrawAt?: string | null;
  /** Real, server-computed "% of members with a verified payment this cycle" (organizer view only). */
  contributionProgressPercent?: number;
  /** Defaults to the member-facing group details route. */
  detailsHref?: string;
  /** Organizer view only: whether the group currently has an active invitation. */
  hasActiveInvitation?: boolean;
  /** Organizer view only: renders an "Invite" button linking here when set. */
  inviteHref?: string;
  /** Organizer view only: renders a "Share" button that runs this when set. */
  onShare?: () => void;
}

export function GroupCard({
  groupId,
  name,
  groupCode,
  contributionAmountPaise,
  currencyCode,
  frequency,
  memberCount,
  maximumMembers,
  status,
  nextDrawAt,
  contributionProgressPercent,
  detailsHref,
  hasActiveInvitation,
  inviteHref,
  onShare,
}: GroupCardProps) {
  const fillRate = maximumMembers > 0 ? (memberCount / maximumMembers) * 100 : 0;

  return (
    <Card className="shadow-sm shadow-primary/5">
      <CardHeader>
        <div className="flex items-start justify-between gap-3">
          <div>
            <CardTitle className="text-base">{name}</CardTitle>
            <p className="text-xs text-muted-foreground">{groupCode}</p>
          </div>
          <div className="flex items-center gap-2">
            {hasActiveInvitation && (
              <Badge variant="outline" className="border-transparent bg-info/10 text-info">
                Invitation pending
              </Badge>
            )}
            {status && <StatusBadge status={status} />}
          </div>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {(contributionAmountPaise !== undefined || frequency) && (
          <div className="grid grid-cols-2 gap-3 text-sm">
            {contributionAmountPaise !== undefined && (
              <div>
                <p className="text-xs text-muted-foreground">Monthly amount</p>
                <p className="font-medium text-foreground">
                  {formatPaiseAsRupees(contributionAmountPaise)}
                  {currencyCode && currencyCode !== "INR" ? ` ${currencyCode}` : ""}
                </p>
              </div>
            )}
            {frequency && (
              <div>
                <p className="text-xs text-muted-foreground">Frequency</p>
                <p className="font-medium text-foreground">{frequency}</p>
              </div>
            )}
          </div>
        )}

        <div className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span className="inline-flex items-center gap-1">
              <Users className="size-3.5" /> {memberCount}/{maximumMembers} members
            </span>
            <span>{Math.round(fillRate)}%</span>
          </div>
          <Progress value={fillRate} />
        </div>

        {nextDrawAt && (
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <CalendarClock className="size-3.5" /> Next draw {formatDateTime(nextDrawAt)}
          </p>
        )}

        {contributionProgressPercent !== undefined && (
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Paid this cycle</span>
              <span>{contributionProgressPercent}%</span>
            </div>
            <Progress value={contributionProgressPercent} />
          </div>
        )}

        {(inviteHref || onShare) && (
          <div className="flex gap-2">
            {inviteHref && (
              <Link href={inviteHref} className={cn(buttonVariants({ variant: "outline" }), "flex-1")}>
                <UserPlus className="size-4" /> Invite
              </Link>
            )}
            {onShare && (
              <Button variant="outline" className="flex-1" onClick={onShare}>
                <MessageCircle className="size-4" /> Share
              </Button>
            )}
          </div>
        )}

        <Link
          href={detailsHref ?? `/dashboard/groups/${groupId}`}
          className={cn(buttonVariants({ variant: "outline" }), "w-full")}
        >
          View details <ArrowRight className="size-4" />
        </Link>
      </CardContent>
    </Card>
  );
}
