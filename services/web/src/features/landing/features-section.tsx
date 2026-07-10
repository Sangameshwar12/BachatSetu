"use client";

import { motion } from "framer-motion";

import { Card, CardContent } from "@/components/ui/card";
import { SectionHeading } from "@/components/shared/section-heading";
import { features } from "@/constants/landing";

export function FeaturesSection() {
  return (
    <section id="features" className="section-y">
      <div className="container-page flex flex-col gap-14">
        <SectionHeading
          eyebrow="Features"
          title="Everything a savings group actually needs"
          description="No spreadsheets, no manual reminders, no disputes about who paid what. Every feature below is live in the product today."
        />

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-80px" }}
              transition={{ duration: 0.4, delay: (index % 4) * 0.06 }}
            >
              <Card className="h-full border-border/60 transition-colors hover:border-primary/40">
                <CardContent className="flex flex-col gap-4 p-6">
                  <span className="inline-flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    <feature.icon className="size-5" strokeWidth={2} />
                  </span>
                  <div className="flex flex-col gap-1.5">
                    <h3 className="font-semibold text-foreground">{feature.title}</h3>
                    <p className="text-sm text-muted-foreground">{feature.description}</p>
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
