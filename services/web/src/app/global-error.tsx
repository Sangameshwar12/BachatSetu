"use client";

import { useEffect } from "react";

import "./globals.css";

/** Next.js App Router convention: catches errors thrown by the root layout itself. */
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <html lang="en">
      <body className="flex min-h-svh flex-col items-center justify-center gap-4 px-4 text-center antialiased">
        <div className="flex flex-col gap-1">
          <h1 className="text-lg font-semibold text-foreground">BachatSetu hit a snag</h1>
          <p className="max-w-sm text-sm text-muted-foreground">
            Something went wrong loading the app. Please try again.
          </p>
        </div>
        <button
          onClick={reset}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
        >
          Try again
        </button>
      </body>
    </html>
  );
}
