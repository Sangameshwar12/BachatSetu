import { Loader2 } from "lucide-react";

/** Next.js App Router convention: shown while a route segment (and its data) is loading. */
export default function GlobalLoading() {
  return (
    <div className="flex min-h-svh items-center justify-center">
      <Loader2 className="size-6 animate-spin text-muted-foreground" />
    </div>
  );
}
