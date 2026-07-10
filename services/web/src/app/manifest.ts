import type { MetadataRoute } from "next";

import { siteConfig } from "@/constants/site";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: siteConfig.name,
    short_name: siteConfig.name,
    description: siteConfig.description,
    start_url: "/dashboard",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#4f46e5",
    icons: [{ src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" }],
    shortcuts: [
      { name: "My Groups", url: "/dashboard/groups" },
      { name: "Payments", url: "/dashboard/payments" },
      { name: "Notifications", url: "/dashboard/notifications" },
    ],
  };
}
