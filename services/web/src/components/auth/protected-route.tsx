"use client";

import { Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { useAuth } from "@/contexts/auth-context";

/**
 * Client-side route guard. There is no server-readable session (tokens live only in
 * localStorage, and the backend issues bearer JWTs rather than a session cookie), so gating
 * happens after mount rather than in `proxy.ts` — the edge/node runtime has no way to read
 * localStorage. Renders a full-screen loading state until the auth check resolves, then either
 * the protected content or (after redirecting) nothing.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading || !isAuthenticated) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return <>{children}</>;
}
