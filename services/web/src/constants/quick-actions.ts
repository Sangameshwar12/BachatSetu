import {
  Bell,
  Receipt,
  Settings,
  UserPlus,
  UserRound,
  Users,
  Wallet,
  type LucideIcon,
} from "lucide-react";

export interface QuickAction {
  label: string;
  href: string;
  icon: LucideIcon;
}

export const dashboardQuickActions: QuickAction[] = [
  { label: "Join Group", href: "/dashboard/groups/join", icon: UserPlus },
  { label: "View Groups", href: "/dashboard/groups", icon: Users },
  { label: "Payments", href: "/dashboard/payments", icon: Wallet },
  { label: "Receipts", href: "/dashboard/receipts", icon: Receipt },
  { label: "Notifications", href: "/dashboard/notifications", icon: Bell },
  { label: "Profile", href: "/dashboard/profile", icon: UserRound },
  { label: "Settings", href: "/dashboard/settings", icon: Settings },
];
