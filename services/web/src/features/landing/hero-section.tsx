"use client";

import { motion, type Variants } from "framer-motion";
import { ArrowRight, BadgeCheck, CalendarClock, ShieldCheck, Sparkles, Trophy, Users } from "lucide-react";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const fadeUp: Variants = {
  hidden: { opacity: 0, y: 16 },
  show: (delay = 0) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, delay, ease: [0.22, 1, 0.36, 1] as const },
  }),
};

export function HeroSection() {
  return (
    <section className="brand-glow relative overflow-hidden pt-16 pb-20 sm:pt-24 sm:pb-28">
      <div className="container-page grid items-center gap-14 lg:grid-cols-2 lg:gap-10">
        <div className="flex flex-col items-start gap-6">
          <motion.span
            variants={fadeUp}
            initial="hidden"
            animate="show"
            className="inline-flex items-center gap-1.5 rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-xs font-medium text-primary"
          >
            <Sparkles className="size-3.5" />
            Digital Bhishi &amp; chit fund management
          </motion.span>

          <motion.h1
            variants={fadeUp}
            initial="hidden"
            animate="show"
            custom={0.08}
            className="text-display font-semibold text-foreground"
          >
            Group savings, run with{" "}
            <span className="bg-gradient-to-r from-primary to-violet-500 bg-clip-text text-transparent">
              complete transparency
            </span>
            .
          </motion.h1>

          <motion.p
            variants={fadeUp}
            initial="hidden"
            animate="show"
            custom={0.16}
            className="max-w-xl text-lg text-muted-foreground"
          >
            BachatSetu brings your Bhishi or chit fund group online — secure OTP sign-in, tracked
            contributions, fair draws and auctions, and instant receipts, so every member always
            knows exactly where things stand.
          </motion.p>

          <motion.div
            variants={fadeUp}
            initial="hidden"
            animate="show"
            custom={0.24}
            className="flex flex-col gap-3 sm:flex-row"
          >
            <Link
              href="/signup"
              className={cn(buttonVariants({ size: "lg" }), "h-11 px-6 text-base")}
            >
              Start your group
              <ArrowRight className="size-4" />
            </Link>
            <Link
              href="#how-it-works"
              className={cn(
                buttonVariants({ size: "lg", variant: "outline" }),
                "h-11 px-6 text-base",
              )}
            >
              See how it works
            </Link>
          </motion.div>

          <motion.div
            variants={fadeUp}
            initial="hidden"
            animate="show"
            custom={0.32}
            className="flex flex-wrap items-center gap-x-6 gap-y-2 pt-2 text-sm text-muted-foreground"
          >
            <span className="inline-flex items-center gap-1.5">
              <ShieldCheck className="size-4 text-primary" /> Secure OTP sign-in
            </span>
            <span className="inline-flex items-center gap-1.5">
              <BadgeCheck className="size-4 text-primary" /> Verified contributions
            </span>
            <span className="inline-flex items-center gap-1.5">
              <Trophy className="size-4 text-primary" /> Transparent draws
            </span>
          </motion.div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 24, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.6, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
          className="relative mx-auto w-full max-w-md"
        >
          <div className="rounded-3xl border border-border/60 bg-card/80 p-6 shadow-2xl shadow-primary/10 backdrop-blur">
            <div className="flex items-center justify-between border-b border-border/60 pb-4">
              <div>
                <p className="text-xs text-muted-foreground">Your group</p>
                <p className="text-sm font-semibold text-foreground">Friends Monthly Bhishi</p>
              </div>
              <span className="inline-flex items-center gap-1 rounded-full bg-success/10 px-2.5 py-1 text-xs font-medium text-success">
                <BadgeCheck className="size-3.5" /> On schedule
              </span>
            </div>

            <div className="grid grid-cols-2 gap-3 py-4">
              <div className="rounded-2xl bg-muted/60 p-4">
                <Users className="mb-2 size-4 text-primary" />
                <p className="text-xs text-muted-foreground">Members</p>
                <p className="text-lg font-semibold text-foreground">Active group</p>
              </div>
              <div className="rounded-2xl bg-muted/60 p-4">
                <CalendarClock className="mb-2 size-4 text-primary" />
                <p className="text-xs text-muted-foreground">Next draw</p>
                <p className="text-lg font-semibold text-foreground">Scheduled</p>
              </div>
            </div>

            <div className="flex items-center justify-between rounded-2xl border border-brand/30 bg-brand-muted px-4 py-3">
              <div className="flex items-center gap-2">
                <Trophy className="size-4 text-brand-foreground" />
                <p className="text-sm font-medium text-brand-foreground">This cycle&apos;s payout</p>
              </div>
              <span className="text-sm font-semibold text-brand-foreground">Draw pending</span>
            </div>
          </div>

          <div
            aria-hidden
            className="absolute -right-6 -bottom-6 -z-10 size-40 rounded-full bg-primary/20 blur-3xl"
          />
        </motion.div>
      </div>
    </section>
  );
}
