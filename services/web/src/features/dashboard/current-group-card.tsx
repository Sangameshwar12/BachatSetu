import { ArrowRight, Users } from "lucide-react";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";
import type { CurrentGroupResponse } from "@/types/dashboard";
import { formatPaiseAsRupees } from "@/utils/format";

export function CurrentGroupCard({ group }: { group: CurrentGroupResponse }) {
  const fillRate = group.maximumMembers > 0 ? (group.memberCount / group.maximumMembers) * 100 : 0;

  return (
    <Card className="shadow-sm shadow-primary/5">
      <CardHeader>
        <div className="flex items-start justify-between gap-3">
          <div>
            <CardTitle className="text-lg">{group.name}</CardTitle>
            <p className="text-xs text-muted-foreground">{group.groupCode}</p>
          </div>
          <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">
            {group.frequency}
          </span>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-2xl bg-muted/60 p-3">
            <p className="text-xs text-muted-foreground">Contribution</p>
            <p className="text-lg font-semibold text-foreground">
              {formatPaiseAsRupees(group.upcomingInstallmentAmountPaise)}
            </p>
          </div>
          <div className="rounded-2xl bg-muted/60 p-3">
            <p className="text-xs text-muted-foreground">Members</p>
            <p className="text-lg font-semibold text-foreground">
              {group.memberCount}/{group.maximumMembers}
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span className="inline-flex items-center gap-1">
              <Users className="size-3.5" /> Group capacity
            </span>
            <span>{Math.round(fillRate)}%</span>
          </div>
          <Progress value={fillRate} />
        </div>

        <Link
          href={`/dashboard/groups/${group.groupId}`}
          className={cn(buttonVariants({ variant: "outline" }), "w-full")}
        >
          View group details <ArrowRight className="size-4" />
        </Link>
      </CardContent>
    </Card>
  );
}
