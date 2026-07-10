"use client";

import { Activity, Database, FileClock, HardDrive, Info, Server } from "lucide-react";

import { PageContainer } from "@/components/dashboard/page-container";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuditSearch } from "@/hooks/use-audit";
import { useStorageAnalytics } from "@/hooks/use-admin-analytics";
import { useSystemHealth } from "@/hooks/use-system-health";
import { cn } from "@/lib/utils";
import type { ComponentHealth } from "@/types/platform-operations";
import { formatBytes, formatDateTime } from "@/utils/format";

function healthBadgeClass(status: string): string {
  const normalized = status.toUpperCase();
  if (normalized === "UP" || normalized === "HEALTHY" || normalized === "OK") return "bg-success/10 text-success";
  if (normalized === "DEGRADED" || normalized === "WARNING") return "bg-warning/10 text-warning";
  return "bg-destructive/10 text-destructive";
}

function ComponentHealthRow({ component }: { component: ComponentHealth }) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-border/60 px-3 py-2">
      <div>
        <p className="text-sm font-medium text-foreground">{component.name}</p>
        {component.detail && <p className="text-xs text-muted-foreground">{component.detail}</p>}
      </div>
      <Badge variant="outline" className={cn("border-transparent", healthBadgeClass(component.status))}>
        {component.status}
      </Badge>
    </div>
  );
}

function formatUptime(seconds: number): string {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${days}d ${hours}h ${minutes}m`;
}

export function AdminMonitoringContent() {
  const health = useSystemHealth();
  const storage = useStorageAnalytics();
  const audit = useAuditSearch({ size: 10 });

  return (
    <PageContainer title="Monitoring" description="Platform health, storage, notifications, and audit activity.">
      <Card>
        <CardHeader>
          <CardTitle>
            <span className="flex items-center gap-2">
              <Server className="size-4" /> System status
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {health.isPending ? (
            <div className="flex flex-col gap-2">
              {Array.from({ length: 3 }, (_, index) => (
                <Skeleton key={index} className="h-12 rounded-lg" />
              ))}
            </div>
          ) : health.isError ? (
            <ErrorState error={health.error} onRetry={() => health.refetch()} />
          ) : (
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <ComponentHealthRow component={health.data.database} />
                <ComponentHealthRow component={health.data.storage} />
                <ComponentHealthRow component={health.data.notification} />
              </div>
              <div className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
                <div>
                  <p className="text-xs text-muted-foreground">Uptime</p>
                  <p className="font-medium text-foreground">{formatUptime(health.data.uptimeSeconds)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Java</p>
                  <p className="font-medium text-foreground">{health.data.javaVersion}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Memory used</p>
                  <p className="font-medium text-foreground">
                    {formatBytes(health.data.usedMemoryBytes)} / {formatBytes(health.data.maxMemoryBytes)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Disk free</p>
                  <p className="font-medium text-foreground">
                    {formatBytes(health.data.usableDiskBytes)} / {formatBytes(health.data.totalDiskBytes)}
                  </p>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>
            <span className="flex items-center gap-2">
              <HardDrive className="size-4" /> Storage usage
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {storage.isPending ? (
            <Skeleton className="h-16 rounded-lg" />
          ) : storage.isError ? (
            <ErrorState error={storage.error} onRetry={() => storage.refetch()} />
          ) : (
            <div className="grid grid-cols-3 gap-4 text-sm">
              <div>
                <p className="text-xs text-muted-foreground">Total files</p>
                <p className="font-medium text-foreground">{storage.data.totalFiles}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Total storage used</p>
                <p className="font-medium text-foreground">{formatBytes(storage.data.totalStorageBytes)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Average file size</p>
                <p className="font-medium text-foreground">{formatBytes(storage.data.averageFileSizeBytes)}</p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>
            <span className="flex items-center gap-2">
              <Activity className="size-4" /> Notification health
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {health.isPending ? (
            <Skeleton className="h-12 rounded-lg" />
          ) : health.isError ? (
            <p className="text-sm text-muted-foreground">Unavailable — see System status above.</p>
          ) : (
            <ComponentHealthRow component={health.data.notification} />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>
            <span className="flex items-center gap-2">
              <FileClock className="size-4" /> Audit overview
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <Alert>
            <Info />
            <AlertTitle>Tenant-scoped, not platform-wide</AlertTitle>
            <AlertDescription>
              The audit search endpoint always scopes to the caller&apos;s own tenant — there is no cross-tenant
              audit endpoint yet. The entries below are real, but only cover this admin account&apos;s tenant.
            </AlertDescription>
          </Alert>

          {audit.isPending ? (
            <div className="flex flex-col gap-2">
              {Array.from({ length: 4 }, (_, index) => (
                <Skeleton key={index} className="h-10 rounded-lg" />
              ))}
            </div>
          ) : audit.isError ? (
            <ErrorState error={audit.error} onRetry={() => audit.refetch()} />
          ) : audit.data.content.length === 0 ? (
            <EmptyState icon={Database} title="No audit entries yet" description="Nothing has been recorded for this tenant yet." />
          ) : (
            <div className="flex flex-col gap-2">
              {audit.data.content.map((entry) => (
                <div key={entry.auditId} className="rounded-lg border border-border/60 px-3 py-2 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-foreground">{entry.eventType}</span>
                    <span className="text-xs text-muted-foreground">{formatDateTime(entry.createdAt)}</span>
                  </div>
                  <p className="text-xs text-muted-foreground">{entry.description}</p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </PageContainer>
  );
}
