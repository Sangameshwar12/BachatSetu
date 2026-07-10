import { ArrowRight } from "lucide-react";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { notificationCategoryMeta } from "@/constants/notification";
import type { DashboardNotificationSummary } from "@/types/dashboard";
import { formatRelativeTime } from "@/utils/format";

export function RecentNotificationsCard({
  notifications,
}: {
  notifications: DashboardNotificationSummary[];
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Recent notifications</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-1">
        {notifications.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nothing new to show yet.</p>
        ) : (
          notifications.slice(0, 5).map((notification) => {
            const meta = notificationCategoryMeta(notification.category);
            const Icon = meta.icon;
            return (
              <div
                key={notification.notificationId}
                className="flex items-center gap-3 rounded-xl px-2 py-2.5 transition-colors hover:bg-muted/60"
              >
                <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <Icon className="size-4" />
                </span>
                <div className="flex min-w-0 flex-1 flex-col">
                  <p className="truncate text-sm font-medium text-foreground">{meta.label}</p>
                  <p className="text-xs text-muted-foreground">{formatRelativeTime(notification.createdAt)}</p>
                </div>
              </div>
            );
          })
        )}
        <Link
          href="/dashboard/notifications"
          className={cn(buttonVariants({ variant: "ghost", size: "sm" }), "mt-1 justify-between")}
        >
          View all notifications <ArrowRight className="size-4" />
        </Link>
      </CardContent>
    </Card>
  );
}
