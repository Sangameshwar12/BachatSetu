"use client";

import { Bell, Info, Search, UserPlus } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";

import { PageContainer } from "@/components/dashboard/page-container";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { notificationCategoryMeta, notificationFilterGroups } from "@/constants/notification";
import { isNoActiveGroupError, useMemberDashboard } from "@/hooks/use-member-dashboard";
import { useReadNotifications } from "@/hooks/use-read-notifications";
import { cn } from "@/lib/utils";
import type { DashboardNotificationSummary } from "@/types/dashboard";
import { formatRelativeTime } from "@/utils/format";

const NO_NOTIFICATIONS: DashboardNotificationSummary[] = [];

export function NotificationsContent() {
  const { data, isPending, isError, error, refetch } = useMemberDashboard();
  const { isRead, markRead } = useReadNotifications();
  const [search, setSearch] = useState("");
  const [activeGroup, setActiveGroup] = useState<string | null>(null);

  const notifications = data?.recentNotifications ?? NO_NOTIFICATIONS;

  const filtered = useMemo(() => {
    return notifications.filter((notification) => {
      const meta = notificationCategoryMeta(notification.category);
      const matchesGroup = !activeGroup || meta.group === activeGroup;
      const matchesSearch =
        search.trim().length === 0 || meta.label.toLowerCase().includes(search.trim().toLowerCase());
      return matchesGroup && matchesSearch;
    });
  }, [notifications, activeGroup, search]);

  if (isPending) {
    return (
      <PageContainer title="Notifications">
        <Skeleton className="h-10 w-full max-w-sm rounded-lg" />
        <Skeleton className="h-72 rounded-2xl" />
      </PageContainer>
    );
  }

  if (isError && !isNoActiveGroupError(error)) {
    return (
      <PageContainer title="Notifications">
        <ErrorState error={error} onRetry={() => refetch()} />
      </PageContainer>
    );
  }

  if (notifications.length === 0) {
    return (
      <PageContainer title="Notifications">
        <EmptyState
          icon={Bell}
          title="No notifications yet"
          description={
            isNoActiveGroupError(error)
              ? "Join a group to start receiving payment, draw, and group updates."
              : "You're all caught up — new activity on your group will show up here."
          }
          action={
            isNoActiveGroupError(error) ? (
              <Link href="/dashboard/groups/join" className={cn(buttonVariants())}>
                <UserPlus className="size-4" /> Join a group
              </Link>
            ) : undefined
          }
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer title="Notifications" description="Payments, draws, and group updates.">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full max-w-sm">
          <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search notifications"
            className="pl-9"
          />
        </div>
        <div className="flex flex-wrap gap-1.5">
          <button
            type="button"
            onClick={() => setActiveGroup(null)}
            className={cn(
              "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
              activeGroup === null
                ? "border-primary bg-primary/10 text-primary"
                : "border-border/60 text-muted-foreground hover:text-foreground"
            )}
          >
            All
          </button>
          {notificationFilterGroups.map((group) => (
            <button
              key={group.value}
              type="button"
              onClick={() => setActiveGroup(group.value)}
              className={cn(
                "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                activeGroup === group.value
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border/60 text-muted-foreground hover:text-foreground"
              )}
            >
              {group.label}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-col divide-y divide-border/60 rounded-2xl border border-border/60 bg-card">
        {filtered.length === 0 ? (
          <p className="px-4 py-10 text-center text-sm text-muted-foreground">
            No notifications match your filters.
          </p>
        ) : (
          filtered.map((notification) => {
            const meta = notificationCategoryMeta(notification.category);
            const Icon = meta.icon;
            const read = isRead(notification.notificationId);
            return (
              <button
                key={notification.notificationId}
                type="button"
                onClick={() => markRead(notification.notificationId)}
                className="flex items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/60"
              >
                <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <Icon className="size-4" />
                </span>
                <div className="flex min-w-0 flex-1 flex-col">
                  <span className="flex items-center gap-2">
                    <span className={cn("truncate text-sm font-medium text-foreground", read && "font-normal")}>
                      {meta.label}
                    </span>
                    {!read && <span className="size-1.5 shrink-0 rounded-full bg-primary" aria-label="Unread" />}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {formatRelativeTime(notification.createdAt)}
                  </span>
                </div>
                <Badge variant="outline" className="shrink-0 text-[10px]">
                  {notification.status}
                </Badge>
              </button>
            );
          })
        )}
      </div>

      <Alert>
        <Info />
        <AlertTitle>Showing your most recent activity</AlertTitle>
        <AlertDescription>
          There&apos;s no backend endpoint yet for a member&apos;s full notification history — only
          a short recent list is available, and &quot;read&quot; state is stored on this device
          only (the Notification domain model has no read/unread status).
        </AlertDescription>
      </Alert>
    </PageContainer>
  );
}
