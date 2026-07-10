import Link from "next/link";

import { dashboardQuickActions } from "@/constants/quick-actions";

export function QuickActionsGrid() {
  return (
    <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 lg:grid-cols-7">
      {dashboardQuickActions.map((action) => {
        const Icon = action.icon;
        return (
          <Link
            key={action.href}
            href={action.href}
            className="group flex flex-col items-center gap-2 rounded-2xl border border-border/60 bg-card px-3 py-4 text-center transition-colors hover:border-primary/40 hover:bg-primary/5 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <span className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
              <Icon className="size-4.5" />
            </span>
            <span className="text-xs font-medium text-foreground">{action.label}</span>
          </Link>
        );
      })}
    </div>
  );
}
