"use client";

import { ArrowRight, Bell } from "lucide-react";
import Link from "next/link";

import { Button, buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { notificationCategoryMeta } from "@/constants/notification";
import { useMemberDashboard } from "@/hooks/use-member-dashboard";
import { cn } from "@/lib/utils";
import { formatRelativeTime } from "@/utils/format";

/**
 * Sourced from `GET /api/v1/dashboard/member`'s `recentNotifications` — the only member-scoped
 * notification data the backend exposes today (no dedicated recipient-filtered list endpoint
 * exists yet). See the Notifications page and Sprint FE-3 report for the full explanation.
 */
export function NotificationBell() {
  const { data } = useMemberDashboard();
  const notifications = data?.recentNotifications ?? [];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={<Button variant="ghost" size="icon" aria-label="Notifications" />}
      >
        <Bell className="size-4.5" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-72">
        <DropdownMenuLabel>Notifications</DropdownMenuLabel>
        {notifications.length === 0 ? (
          <div className="flex flex-col items-center gap-1 px-2 py-8 text-center">
            <p className="text-sm font-medium text-foreground">You&apos;re all caught up</p>
            <p className="text-xs text-muted-foreground">Nothing new to review right now.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-0.5 py-1">
            {notifications.slice(0, 4).map((notification) => {
              const meta = notificationCategoryMeta(notification.category);
              const Icon = meta.icon;
              return (
                <div
                  key={notification.notificationId}
                  className="flex items-center gap-2.5 rounded-md px-2 py-1.5"
                >
                  <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                    <Icon className="size-3.5" />
                  </span>
                  <div className="flex min-w-0 flex-col">
                    <span className="truncate text-sm font-medium text-foreground">{meta.label}</span>
                    <span className="text-xs text-muted-foreground">
                      {formatRelativeTime(notification.createdAt)}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
        <DropdownMenuSeparator />
        <Link
          href="/dashboard/notifications"
          className={cn(buttonVariants({ variant: "ghost", size: "sm" }), "w-full justify-between")}
        >
          View all <ArrowRight className="size-4" />
        </Link>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
