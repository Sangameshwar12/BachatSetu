"use client";

import { ChevronRight } from "lucide-react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { Fragment } from "react";

import { dashboardNavItems } from "@/constants/dashboard";

/** Derives a simple two-level breadcrumb from the current path against the known nav items. */
export function Breadcrumb() {
  const pathname = usePathname();
  const current = dashboardNavItems.find((item) => item.href === pathname);
  const segments = [{ label: "Dashboard", href: "/dashboard" }];

  if (current && current.href !== "/dashboard") {
    segments.push({ label: current.label, href: current.href });
  }

  return (
    <nav aria-label="Breadcrumb" className="flex items-center gap-1.5 text-sm">
      {segments.map((segment, index) => {
        const isLast = index === segments.length - 1;
        return (
          <Fragment key={segment.href}>
            {index > 0 && <ChevronRight className="size-3.5 text-muted-foreground/60" />}
            {isLast ? (
              <span className="font-medium text-foreground">{segment.label}</span>
            ) : (
              <Link href={segment.href} className="text-muted-foreground hover:text-foreground">
                {segment.label}
              </Link>
            )}
          </Fragment>
        );
      })}
    </nav>
  );
}
