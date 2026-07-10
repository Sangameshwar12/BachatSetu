import type { LucideIcon } from "lucide-react";
import {
  Banknote,
  Briefcase,
  Gavel,
  LayoutDashboard,
  LifeBuoy,
  Receipt,
  Settings,
  Users,
} from "lucide-react";

export interface DashboardNavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  /** True once a real frontend page exists for this section. */
  available: boolean;
}

/**
 * Every item here corresponds to a real, already-built backend module — items still marked
 * `available: false` have no group/member-scoped list endpoint to back a standalone page yet
 * (see Sprint FE-3 report). Rather than linking to routes that would 404, unavailable items
 * render disabled with a "Soon" badge until their sprint lands.
 */
export const dashboardNavItems: DashboardNavItem[] = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard, available: true },
  { label: "Groups", href: "/dashboard/groups", icon: Users, available: true },
  { label: "Payments", href: "/dashboard/payments", icon: Banknote, available: true },
  { label: "Receipts", href: "/dashboard/receipts", icon: Receipt, available: true },
  { label: "Draws", href: "/dashboard/draws", icon: Gavel, available: false },
  { label: "Support", href: "/dashboard/support", icon: LifeBuoy, available: false },
  { label: "Settings", href: "/dashboard/settings", icon: Settings, available: true },
  { label: "Organizer", href: "/dashboard/organizer", icon: Briefcase, available: true },
];
