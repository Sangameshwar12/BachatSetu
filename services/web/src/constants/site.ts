/** Global, non-marketing site configuration shared across the app shell and metadata. */
export const siteConfig = {
  name: "BachatSetu",
  tagline: "Group savings, done right.",
  description:
    "BachatSetu is a digital platform for running Bhishi and chit fund savings groups — secure sign-in, transparent contributions, automated draws, and instant receipts, all in one place.",
  url: "https://bachatsetu.example.com",
  supportEmail: "support@bachatsetu.example.com",
} as const;

export type NavLink = {
  label: string;
  href: string;
};

/** Primary marketing-site navigation, shown in the header and mobile drawer. */
export const marketingNavLinks: NavLink[] = [
  { label: "Features", href: "#features" },
  { label: "How it works", href: "#how-it-works" },
  { label: "Benefits", href: "#benefits" },
  { label: "Pricing", href: "#pricing" },
  { label: "FAQ", href: "#faq" },
];

export const footerLinkGroups: { title: string; links: NavLink[] }[] = [
  {
    title: "Product",
    links: [
      { label: "Features", href: "#features" },
      { label: "How it works", href: "#how-it-works" },
      { label: "Pricing", href: "#pricing" },
    ],
  },
  {
    title: "Company",
    links: [
      { label: "FAQ", href: "#faq" },
      { label: "Support", href: "mailto:support@bachatsetu.example.com" },
    ],
  },
  {
    title: "Legal",
    links: [
      { label: "Privacy Policy", href: "/privacy" },
      { label: "Terms of Service", href: "/terms" },
    ],
  },
];
