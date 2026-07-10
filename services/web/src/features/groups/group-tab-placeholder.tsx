import type { LucideIcon } from "lucide-react";

interface GroupTabPlaceholderProps {
  icon: LucideIcon;
  title: string;
  description: string;
}

/** Used for Group Details tabs whose backing list endpoint doesn't exist yet (see FE-3 report). */
export function GroupTabPlaceholder({ icon: Icon, title, description }: GroupTabPlaceholderProps) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-border/60 px-6 py-14 text-center">
      <div className="flex size-11 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <Icon className="size-5" />
      </div>
      <div className="flex flex-col gap-1">
        <h3 className="text-sm font-semibold text-foreground">{title}</h3>
        <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}
