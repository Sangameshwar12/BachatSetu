import { MessagesSquare } from "lucide-react";

import { SectionHeading } from "@/components/shared/section-heading";

/**
 * PENDING BACKEND API: there is no testimonials/reviews endpoint in the BachatSetu backend
 * today. Rather than inventing fake customer quotes and photos, this section ships an honest
 * "coming soon" state. Once a real endpoint exists (e.g. `GET /api/v1/testimonials`), replace
 * the static block below with a `useTestimonials()` React Query hook in
 * `features/landing/hooks/use-testimonials.ts` that fetches and renders real submissions —
 * the surrounding layout is already built to hold that data.
 */
export function TestimonialsSection() {
  return (
    <section className="section-y">
      <div className="container-page flex flex-col items-center gap-8 text-center">
        <SectionHeading
          eyebrow="Community"
          title="Real stories from real groups, coming soon"
          description="We're collecting feedback from the groups running on BachatSetu today. Check back shortly to hear directly from organizers and members."
        />
        <div className="flex max-w-md flex-col items-center gap-3 rounded-2xl border border-dashed border-border/70 bg-muted/30 px-8 py-10">
          <span className="flex size-11 items-center justify-center rounded-full bg-primary/10 text-primary">
            <MessagesSquare className="size-5" />
          </span>
          <p className="text-sm text-muted-foreground">
            Are you running a group on BachatSetu?{" "}
            <a href="mailto:support@bachatsetu.example.com" className="font-medium text-primary hover:underline">
              Share your experience
            </a>{" "}
            and we&apos;ll feature it here.
          </p>
        </div>
      </div>
    </section>
  );
}
