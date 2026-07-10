import type { ReactNode } from "react";

import { DashboardFooter } from "@/components/dashboard/dashboard-footer";
import { Sidebar } from "@/components/dashboard/sidebar";
import { Topbar } from "@/components/dashboard/topbar";

/** The reusable shell every dashboard page renders inside: sidebar + topbar + content + footer. */
export function DashboardShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-svh">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />
        <main id="main-content" className="flex-1">
          {children}
        </main>
        <DashboardFooter />
      </div>
    </div>
  );
}
