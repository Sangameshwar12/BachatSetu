import {
  Banknote,
  Bell,
  Gavel,
  Megaphone,
  ShieldAlert,
  Users,
  type LucideIcon,
} from "lucide-react";

/** Mirrors `notification.domain.model.NotificationCategory` exactly. */
export type NotificationCategory =
  | "VERIFICATION"
  | "PAYMENT_RECEIPT"
  | "CONTRIBUTION_REMINDER"
  | "GROUP_UPDATE"
  | "DRAW_RESULT"
  | "SECURITY_ALERT"
  | "PAYMENT"
  | "RECEIPT"
  | "DRAW"
  | "AUCTION"
  | "GROUP"
  | "MEMBER"
  | "PLATFORM_ANNOUNCEMENT";

/** The sprint's five display groupings, each covering one or more real `NotificationCategory` values. */
export type NotificationFilterGroup = "PAYMENT" | "DRAW" | "REMINDER" | "ANNOUNCEMENT" | "INVITATION";

const CATEGORY_META: Record<NotificationCategory, { label: string; icon: LucideIcon; group: NotificationFilterGroup }> = {
  VERIFICATION: { label: "Verification", icon: ShieldAlert, group: "REMINDER" },
  PAYMENT_RECEIPT: { label: "Payment Receipt", icon: Banknote, group: "PAYMENT" },
  CONTRIBUTION_REMINDER: { label: "Contribution Reminder", icon: Bell, group: "REMINDER" },
  GROUP_UPDATE: { label: "Group Update", icon: Users, group: "ANNOUNCEMENT" },
  DRAW_RESULT: { label: "Draw Result", icon: Gavel, group: "DRAW" },
  SECURITY_ALERT: { label: "Security Alert", icon: ShieldAlert, group: "REMINDER" },
  PAYMENT: { label: "Payment", icon: Banknote, group: "PAYMENT" },
  RECEIPT: { label: "Receipt", icon: Banknote, group: "PAYMENT" },
  DRAW: { label: "Draw", icon: Gavel, group: "DRAW" },
  AUCTION: { label: "Auction", icon: Gavel, group: "DRAW" },
  GROUP: { label: "Group", icon: Users, group: "INVITATION" },
  MEMBER: { label: "Member", icon: Users, group: "ANNOUNCEMENT" },
  PLATFORM_ANNOUNCEMENT: { label: "Announcement", icon: Megaphone, group: "ANNOUNCEMENT" },
};

const FALLBACK_META = { label: "Notification", icon: Bell, group: "ANNOUNCEMENT" as NotificationFilterGroup };

export function notificationCategoryMeta(category: string) {
  return CATEGORY_META[category as NotificationCategory] ?? FALLBACK_META;
}

export const notificationFilterGroups: { value: NotificationFilterGroup; label: string }[] = [
  { value: "PAYMENT", label: "Payment" },
  { value: "DRAW", label: "Draw" },
  { value: "REMINDER", label: "Reminder" },
  { value: "ANNOUNCEMENT", label: "Announcement" },
  { value: "INVITATION", label: "Invitation" },
];
