import { ArrowRight } from "lucide-react";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function CtaSection() {
  return (
    <section className="section-y">
      <div className="container-page">
        <div className="brand-glow relative overflow-hidden rounded-3xl border border-border/60 bg-card px-6 py-16 text-center sm:px-16">
          <div className="relative flex flex-col items-center gap-6">
            <h2 className="text-display max-w-2xl font-semibold text-foreground">
              Bring your savings group online today
            </h2>
            <p className="max-w-md text-muted-foreground">
              It takes a few minutes to verify your number and set up your first group.
            </p>
            <Link
              href="/signup"
              className={cn(buttonVariants({ size: "lg" }), "h-11 px-6 text-base")}
            >
              Get started for free <ArrowRight className="size-4" />
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
