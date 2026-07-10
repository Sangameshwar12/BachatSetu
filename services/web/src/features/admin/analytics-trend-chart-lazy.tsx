"use client";

import dynamic from "next/dynamic";

import { Skeleton } from "@/components/ui/skeleton";

/**
 * Defers loading `recharts` (a sizable dependency) until an analytics tab that actually renders a
 * chart is opened, instead of bundling it into every visit to the Analytics page.
 */
export const AnalyticsTrendChart = dynamic(
  () => import("@/features/admin/analytics-trend-chart").then((mod) => mod.AnalyticsTrendChart),
  { ssr: false, loading: () => <Skeleton className="h-56 w-full rounded-lg" /> }
);
