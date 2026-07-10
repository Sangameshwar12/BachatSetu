import { siteConfig } from "@/constants/site";

export function DashboardFooter() {
  return (
    <footer className="border-t border-border/60 px-4 py-4 text-center text-xs text-muted-foreground sm:px-6 lg:px-8">
      © {new Date().getFullYear()} {siteConfig.name}. All rights reserved.
    </footer>
  );
}
