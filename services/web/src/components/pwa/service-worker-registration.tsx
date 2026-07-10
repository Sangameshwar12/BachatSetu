"use client";

import { useEffect } from "react";

/**
 * Registers the offline-fallback service worker. Renders nothing — this is a side-effect-only
 * component, deliberately isolated from the rest of the tree so a registration failure (e.g.
 * unsupported browser) can never affect app rendering.
 */
export function ServiceWorkerRegistration() {
  useEffect(() => {
    if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
      return;
    }
    navigator.serviceWorker.register("/sw.js").catch(() => {
      // Best-effort only — the app works fully online without it.
    });
  }, []);

  return null;
}
