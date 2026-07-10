import { ArrowRight, Sparkles } from "lucide-react";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { SectionHeading } from "@/components/shared/section-heading";

/**
 * PENDING BACKEND API: the platform has no billing/subscription concept today — payments in
 * the backend are group contributions, not platform fees. Rather than inventing plan tiers and
 * prices, this section states the current, real offer plainly. Replace this with real plan
 * data once a pricing/billing module exists on the backend.
 */
export function PricingSection() {
  return (
    <section id="pricing" className="section-y bg-muted/30">
      <div className="container-page flex flex-col items-center gap-14">
        <SectionHeading eyebrow="Pricing" title="Simple, for now" />

        <Card className="w-full max-w-lg border-primary/20 shadow-lg shadow-primary/5">
          <CardContent className="flex flex-col items-center gap-6 p-10 text-center">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
              <Sparkles className="size-3.5" /> Free while we grow together
            </span>
            <div>
              <p className="text-4xl font-semibold text-foreground">₹0</p>
              <p className="mt-1 text-sm text-muted-foreground">per group, no card required</p>
            </div>
            <ul className="flex flex-col gap-2 text-sm text-muted-foreground">
              <li>Unlimited members per group</li>
              <li>Contributions, draws, auctions, and receipts</li>
              <li>Automated reminders and in-app support</li>
            </ul>
            <Link href="/signup" className={cn(buttonVariants({ size: "lg" }), "w-full")}>
              Create your group <ArrowRight className="size-4" />
            </Link>
            <p className="text-xs text-muted-foreground">
              Plans for large organizers are on our roadmap — reach out if that&apos;s you.
            </p>
          </CardContent>
        </Card>
      </div>
    </section>
  );
}
