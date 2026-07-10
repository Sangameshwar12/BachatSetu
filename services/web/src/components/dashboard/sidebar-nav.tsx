"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { dashboardNavItems } from "@/constants/dashboard";
import { cn } from "@/lib/utils";

export function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();

  return (
    <nav className="flex flex-col gap-1">
      {dashboardNavItems.map((item) => {
        const isActive = pathname === item.href;
        const Icon = item.icon;

        if (!item.available) {
          return (
            <div
              key={item.href}
              className="flex cursor-not-allowed items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground/60"
            >
              <span className="flex items-center gap-2.5">
                <Icon className="size-4" />
                {item.label}
              </span>
              <Badge variant="outline" className="text-[10px] text-muted-foreground/70">
                Soon
              </Badge>
            </div>
          );
        }

        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            className={cn(
              "flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
              isActive
                ? "bg-primary/10 text-primary"
                : "text-muted-foreground hover:bg-muted hover:text-foreground"
            )}
          >
            <Icon className="size-4" />
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
