"use client";

import { motion } from "framer-motion";
import { CheckCircle2 } from "lucide-react";

import { SectionHeading } from "@/components/shared/section-heading";
import { benefits } from "@/constants/landing";

export function BenefitsSection() {
  return (
    <section id="benefits" className="section-y">
      <div className="container-page grid items-center gap-14 lg:grid-cols-2 lg:gap-20">
        <div className="flex flex-col gap-8">
          <SectionHeading
            eyebrow="Benefits"
            title="Why groups switch to BachatSetu"
            align="left"
            description="Whether you organize the group or simply contribute to it, everything you need is in one place."
          />

          <ul className="flex flex-col gap-6">
            {benefits.map((benefit, index) => (
              <motion.li
                key={benefit.title}
                initial={{ opacity: 0, x: -12 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true, margin: "-80px" }}
                transition={{ duration: 0.4, delay: index * 0.06 }}
                className="flex gap-3"
              >
                <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-primary" />
                <div>
                  <p className="font-medium text-foreground">{benefit.title}</p>
                  <p className="text-sm text-muted-foreground">{benefit.description}</p>
                </div>
              </motion.li>
            ))}
          </ul>
        </div>

        <motion.div
          initial={{ opacity: 0, scale: 0.96 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.5 }}
          className="relative"
        >
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2 rounded-2xl border border-border/60 bg-card p-5 shadow-sm">
              <p className="text-xs text-muted-foreground">Organizer dashboard</p>
              <p className="mt-1 font-semibold text-foreground">Every group, one view</p>
              <div className="mt-4 flex gap-2">
                <span className="h-2 flex-1 rounded-full bg-primary/70" />
                <span className="h-2 flex-1 rounded-full bg-primary/40" />
                <span className="h-2 flex-1 rounded-full bg-primary/20" />
              </div>
            </div>
            <div className="rounded-2xl border border-border/60 bg-card p-5 shadow-sm">
              <p className="text-xs text-muted-foreground">Member dashboard</p>
              <p className="mt-2 font-semibold text-foreground">Next draw &amp; latest payment</p>
            </div>
            <div className="rounded-2xl border border-border/60 bg-card p-5 shadow-sm">
              <p className="text-xs text-muted-foreground">Audit trail</p>
              <p className="mt-2 font-semibold text-foreground">Every action, recorded</p>
            </div>
          </div>
          <div
            aria-hidden
            className="absolute -top-8 -left-8 -z-10 size-32 rounded-full bg-brand/20 blur-3xl"
          />
        </motion.div>
      </div>
    </section>
  );
}
