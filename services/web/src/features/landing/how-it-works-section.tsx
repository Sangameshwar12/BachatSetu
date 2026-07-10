"use client";

import { motion } from "framer-motion";

import { SectionHeading } from "@/components/shared/section-heading";
import { howItWorksSteps } from "@/constants/landing";

export function HowItWorksSection() {
  return (
    <section id="how-it-works" className="section-y bg-muted/30">
      <div className="container-page flex flex-col gap-14">
        <SectionHeading
          eyebrow="How it works"
          title="From sign-up to payout, in four steps"
          description="The same flow whether you're starting a brand-new group or joining one a friend already runs."
        />

        <div className="grid gap-8 lg:grid-cols-4 lg:gap-6">
          {howItWorksSteps.map((step, index) => (
            <motion.div
              key={step.title}
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-80px" }}
              transition={{ duration: 0.4, delay: index * 0.08 }}
              className="relative flex flex-col gap-4"
            >
              <div className="flex items-center gap-3">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <span className="inline-flex size-9 items-center justify-center rounded-lg bg-background text-primary shadow-sm">
                  <step.icon className="size-4.5" />
                </span>
              </div>
              <div className="flex flex-col gap-1.5">
                <h3 className="font-semibold text-foreground">{step.title}</h3>
                <p className="text-sm text-muted-foreground">{step.description}</p>
              </div>
              {index < howItWorksSteps.length - 1 ? (
                <div
                  aria-hidden
                  className="absolute top-5 left-[calc(100%-0.75rem)] hidden h-px w-[calc(100%-2.5rem)] bg-gradient-to-r from-border to-transparent lg:block"
                />
              ) : null}
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
