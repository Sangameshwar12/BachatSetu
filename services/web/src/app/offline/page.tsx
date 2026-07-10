import { WifiOff } from "lucide-react";

import { siteConfig } from "@/constants/site";

export const metadata = { title: "You're offline" };

/** Served by the service worker as a fallback for navigation requests made while offline. */
export default function OfflinePage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-4 px-4 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <WifiOff className="size-6" />
      </div>
      <div className="flex flex-col gap-1">
        <h1 className="text-lg font-semibold text-foreground">You&apos;re offline</h1>
        <p className="max-w-sm text-sm text-muted-foreground">
          {siteConfig.name} needs a connection to load your groups and payments. Reconnect and
          reload the page to try again.
        </p>
      </div>
    </div>
  );
}
