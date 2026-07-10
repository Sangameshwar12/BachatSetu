"use client";

import { Building2 } from "lucide-react";
import { useState } from "react";

import { PageContainer } from "@/components/dashboard/page-container";
import { PaginationControls } from "@/components/dashboard/pagination-controls";
import { StatusBadge } from "@/components/dashboard/status-badge";
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
import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { useActivateTenant, useArchiveTenant, usePlatformTenants, useSuspendTenant } from "@/hooks/use-platform-tenants";
import type { TenantResponse } from "@/types/platform-operations";
import { formatCompactNumber, formatDateTime, formatPaiseAsRupees } from "@/utils/format";

const PAGE_SIZE = 20;
const STATUS_OPTIONS = ["ACTIVE", "SUSPENDED", "ARCHIVED"];

function SuspendTenantDialog({ tenant }: { tenant: TenantResponse }) {
  const [reason, setReason] = useState("");
  const suspendTenant = useSuspendTenant();

  return (
    <AlertDialog>
      <AlertDialogTrigger render={<Button variant="destructive" size="sm" />}>Suspend</AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Suspend {tenant.tenantId}?</AlertDialogTitle>
          <AlertDialogDescription>
            Every user in this tenant will lose access until it is reactivated. A reason is required.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <Textarea
          placeholder="Reason for suspension"
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          maxLength={500}
        />
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            disabled={reason.trim().length === 0 || suspendTenant.isPending}
            onClick={() => suspendTenant.mutate({ tenantId: tenant.tenantId, request: { reason: reason.trim() } })}
          >
            Suspend
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

function TenantActions({ tenant }: { tenant: TenantResponse }) {
  const activateTenant = useActivateTenant();
  const archiveTenant = useArchiveTenant();

  if (tenant.status === "ARCHIVED") {
    return <span className="text-xs text-muted-foreground">No further actions</span>;
  }

  return (
    <div className="flex justify-end gap-2">
      {tenant.status === "SUSPENDED" ? (
        <Button variant="outline" size="sm" onClick={() => activateTenant.mutate(tenant.tenantId)} disabled={activateTenant.isPending}>
          Activate
        </Button>
      ) : (
        <SuspendTenantDialog tenant={tenant} />
      )}

      <AlertDialog>
        <AlertDialogTrigger render={<Button variant="outline" size="sm" />}>Archive</AlertDialogTrigger>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Archive {tenant.tenantId}?</AlertDialogTitle>
            <AlertDialogDescription>
              This is a permanent lifecycle change. Archived tenants cannot be reactivated from here.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              onClick={() => archiveTenant.mutate(tenant.tenantId)}
              disabled={archiveTenant.isPending}
            >
              Archive
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

export function AdminTenantsContent() {
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error, refetch } = usePlatformTenants(status, page, PAGE_SIZE);

  return (
    <PageContainer title="Tenant Management" description="Every tenant known to the platform, with per-tenant statistics.">
      <Select
        value={status}
        onValueChange={(value) => {
          setPage(0);
          setStatus(value === "ALL" ? undefined : (value as string));
        }}
      >
        <SelectTrigger className="sm:w-48">
          <SelectValue placeholder="All statuses" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ALL">All statuses</SelectItem>
          {STATUS_OPTIONS.map((option) => (
            <SelectItem key={option} value={option}>
              {option}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {isPending ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, index) => (
            <Skeleton key={index} className="h-40 rounded-2xl" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Building2} title="No tenants match this filter" description="Try clearing the status filter." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map((tenant) => (
            <Card key={tenant.tenantId}>
              <CardContent className="flex flex-col gap-3">
                <div className="flex items-start justify-between gap-2">
                  <p className="truncate font-mono text-xs text-muted-foreground">{tenant.tenantId}</p>
                  <StatusBadge status={tenant.status} />
                </div>
                <div className="grid grid-cols-2 gap-x-3 gap-y-2 text-sm">
                  <div>
                    <p className="text-xs text-muted-foreground">Users</p>
                    <p className="font-medium text-foreground">{formatCompactNumber(tenant.users)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground">Groups</p>
                    <p className="font-medium text-foreground">{formatCompactNumber(tenant.groups)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground">Payments</p>
                    <p className="font-medium text-foreground">{formatCompactNumber(tenant.payments)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground">Revenue</p>
                    <p className="font-medium text-foreground">{formatPaiseAsRupees(tenant.revenuePaise)}</p>
                  </div>
                </div>
                {tenant.suspensionReason && (
                  <p className="text-xs text-warning">Suspended: {tenant.suspensionReason}</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Last activity: {tenant.lastActivityAt ? formatDateTime(tenant.lastActivityAt) : "No activity recorded"}
                </p>
                <TenantActions tenant={tenant} />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {data && (
        <PaginationControls
          page={data.page}
          totalPages={data.totalPages}
          hasNext={data.hasNext}
          hasPrevious={data.hasPrevious}
          onPageChange={setPage}
        />
      )}
    </PageContainer>
  );
}
