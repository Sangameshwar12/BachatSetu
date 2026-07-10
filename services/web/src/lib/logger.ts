/**
 * Vendor-neutral logging/error-reporting abstraction. Every call site in the app should go
 * through here rather than calling `console.*` directly, so a real provider (Sentry, Datadog,
 * etc.) can be wired in later by changing only this file's implementation — never the call
 * sites. Defaults to `console` so behavior today is unchanged from before this abstraction
 * existed.
 */

type LogContext = Record<string, unknown>;

function shouldLog(): boolean {
  return process.env.NODE_ENV !== "test";
}

export const logger = {
  debug(message: string, context?: LogContext): void {
    if (shouldLog()) {
      console.debug(message, context ?? "");
    }
  },
  info(message: string, context?: LogContext): void {
    if (shouldLog()) {
      console.info(message, context ?? "");
    }
  },
  warn(message: string, context?: LogContext): void {
    if (shouldLog()) {
      console.warn(message, context ?? "");
    }
  },
  /**
   * Reports an unexpected error. This is the single seam a monitoring vendor's SDK (e.g.
   * `Sentry.captureException`) would be plugged into — everything upstream of this file stays
   * exactly the same.
   */
  captureException(error: unknown, context?: LogContext): void {
    if (shouldLog()) {
      console.error(error, context ?? "");
    }
  },
} as const;
