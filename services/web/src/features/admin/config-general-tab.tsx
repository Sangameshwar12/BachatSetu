"use client";

import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminConfiguration, useUpdateAdminConfiguration } from "@/hooks/use-admin-config";
import type { UpdateConfigurationRequest } from "@/types/admin-config";

export function ConfigGeneralTab() {
  const { data, isPending, isError, error, refetch } = useAdminConfiguration();
  const updateConfiguration = useUpdateAdminConfiguration();
  const [form, setForm] = useState<UpdateConfigurationRequest | null>(null);

  // One-time hydration of editable form state from the fetched configuration snapshot.
  useEffect(() => {
    if (data) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setForm({
        defaultLanguage: data.defaultLanguage,
        otpExpirySeconds: data.otpExpirySeconds,
        defaultStorageProvider: data.defaultStorageProvider,
        defaultPaymentProvider: data.defaultPaymentProvider,
        notificationRetryCount: data.notificationRetryCount,
        maximumUploadSizeBytes: data.maximumUploadSizeBytes,
        maximumMembersPerGroup: data.maximumMembersPerGroup,
        maximumGroupsPerOrganizer: data.maximumGroupsPerOrganizer,
        maintenanceEnabled: data.maintenanceEnabled,
        maintenanceMessage: data.maintenanceMessage,
        maintenanceStartAt: data.maintenanceStartAt,
        maintenanceEndAt: data.maintenanceEndAt,
      });
    }
  }, [data]);

  if (isPending || !form) {
    return (
      <div className="flex flex-col gap-3">
        {Array.from({ length: 4 }, (_, index) => (
          <Skeleton key={index} className="h-10 rounded-lg" />
        ))}
      </div>
    );
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />;
  }

  return (
    <form
      className="flex max-w-xl flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        updateConfiguration.mutate(form);
      }}
    >
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="defaultLanguage">Default language</Label>
        <Input
          id="defaultLanguage"
          value={form.defaultLanguage}
          onChange={(event) => setForm({ ...form, defaultLanguage: event.target.value })}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="otpExpirySeconds">OTP expiry (seconds)</Label>
        <Input
          id="otpExpirySeconds"
          type="number"
          min={1}
          value={form.otpExpirySeconds}
          onChange={(event) => setForm({ ...form, otpExpirySeconds: Number(event.target.value) })}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="defaultStorageProvider">Default storage provider</Label>
        <Input
          id="defaultStorageProvider"
          value={form.defaultStorageProvider}
          onChange={(event) => setForm({ ...form, defaultStorageProvider: event.target.value })}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="defaultPaymentProvider">Default payment provider</Label>
        <Input
          id="defaultPaymentProvider"
          value={form.defaultPaymentProvider}
          onChange={(event) => setForm({ ...form, defaultPaymentProvider: event.target.value })}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="notificationRetryCount">Notification retry count</Label>
        <Input
          id="notificationRetryCount"
          type="number"
          min={0}
          value={form.notificationRetryCount}
          onChange={(event) => setForm({ ...form, notificationRetryCount: Number(event.target.value) })}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="maximumUploadSizeBytes">Maximum upload size (bytes)</Label>
        <Input
          id="maximumUploadSizeBytes"
          type="number"
          min={1}
          value={form.maximumUploadSizeBytes}
          onChange={(event) => setForm({ ...form, maximumUploadSizeBytes: Number(event.target.value) })}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="maximumMembersPerGroup">Maximum members per group</Label>
        <Input
          id="maximumMembersPerGroup"
          type="number"
          min={1}
          value={form.maximumMembersPerGroup}
          onChange={(event) => setForm({ ...form, maximumMembersPerGroup: Number(event.target.value) })}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="maximumGroupsPerOrganizer">Maximum groups per organizer</Label>
        <Input
          id="maximumGroupsPerOrganizer"
          type="number"
          min={1}
          value={form.maximumGroupsPerOrganizer}
          onChange={(event) => setForm({ ...form, maximumGroupsPerOrganizer: Number(event.target.value) })}
        />
      </div>

      <Button type="submit" disabled={updateConfiguration.isPending} className="w-fit">
        Save changes
      </Button>
      {updateConfiguration.isSuccess && (
        <p className="text-xs text-success">Configuration updated (version {data.version + 1}).</p>
      )}
      {updateConfiguration.isError && (
        <p className="text-xs text-destructive">Couldn&apos;t save — someone may have changed this concurrently.</p>
      )}
    </form>
  );
}
