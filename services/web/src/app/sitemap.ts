import type { MetadataRoute } from "next";

import { siteConfig } from "@/constants/site";

/** Public, unauthenticated routes only — every `/dashboard/*` route requires a session and is excluded. */
export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date();
  return [
    { url: siteConfig.url, lastModified: now, changeFrequency: "weekly", priority: 1 },
    { url: `${siteConfig.url}/privacy`, lastModified: now, changeFrequency: "yearly", priority: 0.3 },
    { url: `${siteConfig.url}/terms`, lastModified: now, changeFrequency: "yearly", priority: 0.3 },
    { url: `${siteConfig.url}/login`, lastModified: now, changeFrequency: "yearly", priority: 0.2 },
    { url: `${siteConfig.url}/signup`, lastModified: now, changeFrequency: "yearly", priority: 0.5 },
  ];
}
