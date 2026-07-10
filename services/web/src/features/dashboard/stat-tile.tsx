import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

interface StatTileProps {
  icon: LucideIcon;
  label: string;
  value: ReactNode;
  hint?: string;
  comingSoon?: boolean;
}

export function StatTile({ icon: Icon, label, value, hint, comingSoon }: StatTileProps) {
  return (
    <div
      className={cn(
        "flex flex-col gap-2 rounded-2xl border border-border/60 bg-card p-4",
        comingSoon && "border-dashed"
      )}
    >
      <span className="flex size-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
        <Icon className="size-4.5" />
      </span>
      <div>
        <p className="text-xs text-muted-foreground">{label}</p>
        {comingSoon ? (
          <p className="text-sm font-medium text-muted-foreground/80">Coming soon</p>
        ) : (
          <p className="text-lg font-semibold text-foreground">{value}</p>
        )}
      </div>
      {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}
