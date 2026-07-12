import { ArrowRight, Sparkles, UserPlus, Users } from "lucide-react";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/** Shown when `GET /api/v1/dashboard/member` returns 404 `no-active-group` — the backend's own
 * documented signal to show a Welcome Screen instead of a dashboard. */
export function WelcomeEmptyState({ mobileNumber }: { mobileNumber?: string }) {
  return (
    <div className="flex flex-col items-center gap-6 rounded-3xl border border-dashed border-border/60 bg-card px-6 py-16 text-center">
      <div className="flex size-14 items-center justify-center rounded-full bg-primary/10 text-primary">
        <Sparkles className="size-6" />
      </div>
      <div className="flex flex-col gap-1.5">
        <h1 className="text-xl font-semibold text-foreground">
          Welcome{mobileNumber ? `, ${mobileNumber}` : ""}
        </h1>
        <p className="max-w-sm text-sm text-muted-foreground">
          You haven&apos;t joined a savings group yet. Create one, or join with a code, link, or QR
          from someone who already has one.
        </p>
      </div>
      <div className="flex flex-col gap-3 sm:flex-row">
        <Link href="/dashboard/groups/join" className={cn(buttonVariants({ size: "lg" }))}>
          <UserPlus className="size-4" /> Join a group
        </Link>
        <Link href="/dashboard/groups" className={cn(buttonVariants({ variant: "outline", size: "lg" }))}>
          <Users className="size-4" /> My groups <ArrowRight className="size-4" />
        </Link>
      </div>
    </div>
  );
}
