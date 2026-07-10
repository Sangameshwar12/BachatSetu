"use client";

import { useEffect, useState } from "react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminConfiguration, useUpdateAdminConfiguration } from "@/hooks/use-admin-config";

export function ConfigMaintenanceTab() {
  const { data, isPending, isError, error, refetch } = useAdminConfiguration();
  const updateConfiguration = useUpdateAdminConfiguration();
  const [enabled, setEnabled] = useState(false);
  const [message, setMessage] = useState("");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");

  // One-time hydration of editable form state from the fetched configuration snapshot.
  useEffect(() => {
    if (data) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setEnabled(data.maintenanceEnabled);
      setMessage(data.maintenanceMessage ?? "");
      setStartAt(data.maintenanceStartAt ?? "");
      setEndAt(data.maintenanceEndAt ?? "");
    }
  }, [data]);

  if (isPending) {
    return (
      <div className="flex flex-col gap-3">
        {Array.from({ length: 3 }, (_, index) => (
          <Skeleton key={index} className="h-10 rounded-lg" />
        ))}
      </div>
    );
  }

  if (isError || !data) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  return (
    <form
      className="flex max-w-xl flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        updateConfiguration.mutate({
          defaultLanguage: data.defaultLanguage,
          otpExpirySeconds: data.otpExpirySeconds,
          defaultStorageProvider: data.defaultStorageProvider,
          defaultPaymentProvider: data.defaultPaymentProvider,
          notificationRetryCount: data.notificationRetryCount,
          maximumUploadSizeBytes: data.maximumUploadSizeBytes,
          maximumMembersPerGroup: data.maximumMembersPerGroup,
          maximumGroupsPerOrganizer: data.maximumGroupsPerOrganizer,
          maintenanceEnabled: enabled,
          maintenanceMessage: message || null,
          maintenanceStartAt: startAt || null,
          maintenanceEndAt: endAt || null,
        });
      }}
    >
      {enabled && (
        <Alert variant="destructive">
          <AlertTitle>Maintenance mode is ON</AlertTitle>
          <AlertDescription>
            Non-admin requests are being blocked platform-wide by the existing maintenance-mode filter.
          </AlertDescription>
        </Alert>
      )}

      <div className="flex items-center justify-between rounded-lg border border-border/60 px-3 py-2">
        <Label htmlFor="maintenanceEnabled">Maintenance mode</Label>
        <Switch id="maintenanceEnabled" checked={enabled} onCheckedChange={setEnabled} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="maintenanceMessage">Message shown to users</Label>
        <Textarea
          id="maintenanceMessage"
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          placeholder="e.g. BachatSetu is undergoing scheduled maintenance."
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="maintenanceStartAt">Starts at (ISO timestamp)</Label>
          <Input
            id="maintenanceStartAt"
            value={startAt}
            onChange={(event) => setStartAt(event.target.value)}
            placeholder="2026-07-10T18:00:00Z"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="maintenanceEndAt">Ends at (ISO timestamp)</Label>
          <Input
            id="maintenanceEndAt"
            value={endAt}
            onChange={(event) => setEndAt(event.target.value)}
            placeholder="2026-07-10T20:00:00Z"
          />
        </div>
      </div>

      <Button type="submit" disabled={updateConfiguration.isPending} className="w-fit">
        Save changes
      </Button>
      {updateConfiguration.isSuccess && <p className="text-xs text-success">Maintenance settings updated.</p>}
    </form>
  );
}
