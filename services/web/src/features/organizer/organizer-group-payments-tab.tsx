import { Info } from "lucide-react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import type { OrganizerGroupResponse } from "@/types/organizer-dashboard";

/**
 * `contributionProgressPercent` is the only real, per-group payment signal the backend exposes
 * (share of active members with at least one verified payment this cycle) — there's no
 * groupId-filtered payment list endpoint, so a full pending/verified/failed breakdown can't be
 * shown safely (see FE-4 report).
 */
export function OrganizerGroupPaymentsTab({ group }: { group: OrganizerGroupResponse | undefined }) {
  return (
    <div className="flex flex-col gap-4">
      {group && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Contribution progress</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>Members with a verified payment this cycle</span>
              <span className="font-medium text-foreground">{group.contributionProgressPercent}%</span>
            </div>
            <Progress value={group.contributionProgressPercent} />
          </CardContent>
        </Card>
      )}
      <Alert>
        <Info />
        <AlertTitle>A full payment breakdown isn&apos;t available yet</AlertTitle>
        <AlertDescription>
          The Payments API doesn&apos;t yet support filtering by group, so pending, verified, and
          failed payments for this specific group can&apos;t be listed individually — only the
          aggregate progress above is available.
        </AlertDescription>
      </Alert>
    </div>
  );
}
