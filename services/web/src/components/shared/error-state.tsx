"use client";

import { AlertTriangle, RefreshCw, WifiOff } from "lucide-react";

import { Button } from "@/components/ui/button";
import { ApiError } from "@/services/api-client";

interface ErrorStateProps {
  error: unknown;
  onRetry?: () => void;
  className?: string;
}

/** Distinguishes a network/connection failure from a backend error, matching what actually happened. */
function describeError(error: unknown): { title: string; description: string; isConnectionLost: boolean } {
  if (error instanceof ApiError) {
    return {
      title: "Something went wrong",
      description: error.message || "The server couldn't complete this request.",
      isConnectionLost: false,
    };
  }
  return {
    title: "Connection lost",
    description: "Couldn't reach BachatSetu. Check your connection and try again.",
    isConnectionLost: true,
  };
}

export function ErrorState({ error, onRetry, className }: ErrorStateProps) {
  const { title, description, isConnectionLost } = describeError(error);
  const Icon = isConnectionLost ? WifiOff : AlertTriangle;

  return (
    <div
      role="alert"
      className={`flex flex-col items-center gap-3 rounded-2xl border border-dashed border-destructive/30 bg-destructive/5 px-6 py-16 text-center ${className ?? ""}`}
    >
      <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
        <Icon className="size-5" />
      </div>
      <div className="flex flex-col gap-1">
        <h2 className="text-base font-semibold text-foreground">{title}</h2>
        <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
      </div>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCw className="size-4" />
          Try again
        </Button>
      )}
    </div>
  );
}
