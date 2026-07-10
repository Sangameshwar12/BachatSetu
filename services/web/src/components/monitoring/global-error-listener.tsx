"use client";

import { useEffect } from "react";

import { logger } from "@/lib/logger";

/**
 * Catches errors React's own boundaries never see: unhandled promise rejections, and script
 * errors thrown outside a React render (event handlers, timers, third-party scripts). Renders
 * nothing — reporting only.
 */
export function GlobalErrorListener() {
  useEffect(() => {
    function handleRejection(event: PromiseRejectionEvent) {
      logger.captureException(event.reason, { source: "unhandledrejection" });
    }
    function handleError(event: ErrorEvent) {
      logger.captureException(event.error ?? event.message, { source: "window.onerror" });
    }

    window.addEventListener("unhandledrejection", handleRejection);
    window.addEventListener("error", handleError);
    return () => {
      window.removeEventListener("unhandledrejection", handleRejection);
      window.removeEventListener("error", handleError);
    };
  }, []);

  return null;
}
