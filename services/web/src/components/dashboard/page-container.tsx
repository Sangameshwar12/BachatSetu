import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

interface PageContainerProps {
  title?: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}

/** Consistent max-width/padding wrapper reused by every dashboard page. */
export function PageContainer({ title, description, actions, children, className }: PageContainerProps) {
  return (
    <div className={cn("mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8", className)}>
      {(title || actions) && (
        <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            {title && <h1 className="text-xl font-semibold text-foreground">{title}</h1>}
            {description && <p className="text-sm text-muted-foreground">{description}</p>}
          </div>
          {actions}
        </div>
      )}
      {children}
    </div>
  );
}
