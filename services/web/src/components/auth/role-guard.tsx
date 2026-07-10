"use client";

import { ShieldAlert } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

import { buttonVariants } from "@/components/ui/button";
import { useAuth } from "@/contexts/auth-context";
import { cn } from "@/lib/utils";

/**
 * Client-side UX gate for role-restricted sections (e.g. the Admin portal). This is not the
 * authorization boundary — every underlying endpoint independently enforces its own role check
 * server-side and returns 403 regardless of what this component does. It only spares a user
 * without the role from navigating into a section that would just show a wall of "Forbidden"
 * error states.
 */
export function RoleGuard({ role, children }: { role: string; children: ReactNode }) {
  const { hasRole } = useAuth();

  if (!hasRole(role)) {
    return (
      <div className="flex min-h-[60svh] flex-col items-center justify-center gap-4 px-4 text-center">
        <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
          <ShieldAlert className="size-6" />
        </div>
        <div className="flex flex-col gap-1">
          <h1 className="text-lg font-semibold text-foreground">You don&apos;t have access to this section</h1>
          <p className="max-w-sm text-sm text-muted-foreground">
            This area is restricted to platform administrators.
          </p>
        </div>
        <Link href="/dashboard" className={cn(buttonVariants())}>
          Back to dashboard
        </Link>
      </div>
    );
  }

  return <>{children}</>;
}
