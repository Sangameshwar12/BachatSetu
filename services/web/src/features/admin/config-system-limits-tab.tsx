"use client";

import { Gauge } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useSystemLimits, useUpdateSystemLimits } from "@/hooks/use-admin-config";
import { formatDateTime } from "@/utils/format";

export function ConfigSystemLimitsTab() {
  const { data, isPending, isError, error, refetch } = useSystemLimits();
  const updateLimits = useUpdateSystemLimits();
  const [drafts, setDrafts] = useState<Record<string, string>>({});

  if (isPending) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 4 }, (_, index) => (
          <Skeleton key={index} className="h-14 rounded-lg" />
        ))}
      </div>
    );
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  if (data.length === 0) {
    return (
      <EmptyState
        icon={Gauge}
        title="No system limits configured"
        description="No platform-wide ceilings have a limit defined in the backend yet."
      />
    );
  }

  return (
    <div className="flex max-w-xl flex-col gap-3">
      {data.map((limit) => {
        const draft = drafts[limit.key] ?? String(limit.value);
        const isDirty = draft !== String(limit.value);
        return (
          <div key={limit.key} className="flex flex-col gap-1.5 rounded-lg border border-border/60 px-3 py-2.5">
            <Label htmlFor={`limit-${limit.key}`}>{limit.key}</Label>
            <div className="flex items-center gap-2">
              <Input
                id={`limit-${limit.key}`}
                type="number"
                value={draft}
                onChange={(event) => setDrafts({ ...drafts, [limit.key]: event.target.value })}
                className="max-w-40"
              />
              <Button
                size="sm"
                variant="outline"
                disabled={!isDirty || updateLimits.isPending || draft.trim() === ""}
                onClick={() =>
                  updateLimits.mutate(
                    { limits: { [limit.key]: Number(draft) } },
                    { onSuccess: () => setDrafts((prev) => ({ ...prev, [limit.key]: draft })) }
                  )
                }
              >
                Save
              </Button>
            </div>
            {limit.updatedAt && (
              <p className="text-xs text-muted-foreground">
                Last changed {formatDateTime(limit.updatedAt)}
                {limit.updatedBy ? ` by ${limit.updatedBy}` : ""}
              </p>
            )}
          </div>
        );
      })}
    </div>
  );
}
