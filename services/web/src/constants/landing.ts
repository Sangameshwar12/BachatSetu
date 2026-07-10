import {
  ShieldCheck,
  Users,
  Wallet,
  Trophy,
  Bell,
  FileText,
  QrCode,
  ClipboardCheck,
  CalendarClock,
  Landmark,
  LifeBuoy,
  type LucideIcon,
} from "lucide-react";

/**
 * Every claim below maps to a capability that already exists in the BachatSetu backend
 * (auth, group, member, draw, payment, receipt, notification, invitation, and support modules).
 * Nothing here describes a feature that isn't implemented.
 */

export type Feature = {
  icon: LucideIcon;
  title: string;
  description: string;
};

export const features: Feature[] = [
  {
    icon: ShieldCheck,
    title: "Secure by design",
    description:
      "Phone number and OTP sign-in, JWT-based sessions with automatic token refresh, and a full audit trail of every action taken in your group.",
  },
  {
    icon: Users,
    title: "Groups that run themselves",
    description:
      "Create a savings group in minutes, set the contribution amount and cycle, and manage members with organizer and co-organizer roles.",
  },
  {
    icon: QrCode,
    title: "Invite in seconds",
    description:
      "Bring members in with a shareable link, a QR code, or a short join code — no manual approvals or spreadsheets.",
  },
  {
    icon: Wallet,
    title: "Transparent contributions",
    description:
      "Every payment is tracked against its cycle and verified before it counts, so the whole group always knows where things stand.",
  },
  {
    icon: Trophy,
    title: "Fair draws and auctions",
    description:
      "Run the monthly payout as a scheduled draw or an open auction, with results recorded and visible to every member.",
  },
  {
    icon: FileText,
    title: "Instant, downloadable receipts",
    description:
      "Every verified payment generates a receipt automatically, ready to view or download as a PDF.",
  },
  {
    icon: Bell,
    title: "Timely reminders",
    description:
      "Automated notifications for upcoming and overdue contributions, draw results, and group updates — so nobody misses a cycle.",
  },
  {
    icon: LifeBuoy,
    title: "Real support, not a black box",
    description:
      "Raise a support ticket in-app and track it through to resolution, with priority and status visible at every step.",
  },
];

export type Step = {
  icon: LucideIcon;
  title: string;
  description: string;
};

export const howItWorksSteps: Step[] = [
  {
    icon: ClipboardCheck,
    title: "Verify your number",
    description:
      "Sign up with your phone number and a one-time password. No passwords to remember, no paperwork to fill out.",
  },
  {
    icon: Landmark,
    title: "Create or join a group",
    description:
      "Start a new savings group and set the contribution and schedule, or join an existing one with a code, QR, or link.",
  },
  {
    icon: CalendarClock,
    title: "Contribute every cycle",
    description:
      "Make your contribution each cycle and get an instant, verifiable receipt. Reminders keep everyone on track.",
  },
  {
    icon: Trophy,
    title: "Receive the payout",
    description:
      "Each cycle's draw or auction decides who receives the pooled amount — recorded transparently for the whole group to see.",
  },
];

export type Benefit = {
  title: string;
  description: string;
};

export const benefits: Benefit[] = [
  {
    title: "No more manual bookkeeping",
    description:
      "Contributions, receipts, and draw history are all recorded automatically — no ledgers, no spreadsheets, no disputes.",
  },
  {
    title: "Built for organizers",
    description:
      "A dedicated organizer dashboard shows every group at a glance: membership, contribution progress, and the next scheduled draw.",
  },
  {
    title: "Built for members",
    description:
      "Members get their own dashboard with their current group, next draw date, latest payment status, and recent notifications.",
  },
  {
    title: "Nothing happens silently",
    description:
      "Every sensitive action — logins, payments, draws, group changes — is recorded in an audit trail your organizer can review.",
  },
];

export type FaqItem = {
  question: string;
  answer: string;
};

export const faqItems: FaqItem[] = [
  {
    question: "How do I join a savings group?",
    answer:
      "An organizer can invite you with a join code, a QR code, or a shareable link. Open it, sign in with your phone number and OTP, and you're in.",
  },
  {
    question: "How is the payout order decided?",
    answer:
      "Your organizer sets up either a scheduled draw or an auction for each cycle. Results are recorded on the group's timeline for every member to see.",
  },
  {
    question: "What happens after I make a contribution?",
    answer:
      "Once your payment is verified, a receipt is generated automatically and is available to view or download as a PDF at any time.",
  },
  {
    question: "Will I be reminded about upcoming payments?",
    answer:
      "Yes. BachatSetu sends automated reminders ahead of due dates, and again if a contribution becomes overdue.",
  },
  {
    question: "Is my data secure?",
    answer:
      "Sign-in uses phone number verification with OTP and JWT-secured sessions with automatic refresh. Every significant action is recorded in an audit trail.",
  },
  {
    question: "What if I run into a problem?",
    answer:
      "You can raise a support ticket directly from the app and track its status until it's resolved by the BachatSetu team.",
  },
];
