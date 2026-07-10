import { CalendarClock, Trophy } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatusBadge } from "@/components/dashboard/status-badge";
import type { NextDrawResponse } from "@/types/dashboard";
import { formatDateTime } from "@/utils/format";

export function NextDrawCard({ draw }: { draw: NextDrawResponse | null }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Trophy className="size-4 text-primary" /> Next draw
        </CardTitle>
      </CardHeader>
      <CardContent>
        {draw ? (
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2 text-sm text-foreground">
              <CalendarClock className="size-4 text-muted-foreground" />
              {formatDateTime(draw.scheduledAt)}
            </div>
            <StatusBadge status={draw.status} />
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">No draw scheduled for your group yet.</p>
        )}
      </CardContent>
    </Card>
  );
}
