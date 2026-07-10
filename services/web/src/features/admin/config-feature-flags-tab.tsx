"use client";

import { Flag } from "lucide-react";
import { toast } from "sonner";

import { ErrorState } from "@/components/shared/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { useFeatureFlags, useUpdateFeatureFlags } from "@/hooks/use-admin-config";
import { ApiError } from "@/services/api-client";
import { formatDateTime } from "@/utils/format";

export function ConfigFeatureFlagsTab() {
  const { data, isPending, isError, error, refetch } = useFeatureFlags();
  const updateFlags = useUpdateFeatureFlags();

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
        icon={Flag}
        title="No feature flags configured"
        description="No platform features have a flag defined in the backend yet."
      />
    );
  }

  return (
    <div className="flex max-w-xl flex-col gap-2">
      {data.map((flag) => (
        <div key={flag.key} className="flex items-center justify-between rounded-lg border border-border/60 px-3 py-2.5">
          <div>
            <Label htmlFor={`flag-${flag.key}`}>{flag.key}</Label>
            {flag.updatedAt && (
              <p className="text-xs text-muted-foreground">
                Last changed {formatDateTime(flag.updatedAt)}
                {flag.updatedBy ? ` by ${flag.updatedBy}` : ""}
              </p>
            )}
          </div>
          <Switch
            id={`flag-${flag.key}`}
            checked={flag.enabled}
            disabled={updateFlags.isPending}
            onCheckedChange={(checked) =>
              updateFlags.mutate(
                { flags: { [flag.key]: checked } },
                {
                  onSuccess: () => toast.success(`${flag.key} ${checked ? "enabled" : "disabled"}.`),
                  onError: (cause) =>
                    toast.error(cause instanceof ApiError ? cause.message : `Couldn't update ${flag.key}.`),
                }
              )
            }
          />
        </div>
      ))}
    </div>
  );
}
