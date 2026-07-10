import { Logo } from "@/components/shared/logo";
import { SidebarNav } from "@/components/dashboard/sidebar-nav";

export function Sidebar() {
  return (
    <aside className="hidden w-64 shrink-0 flex-col border-r border-border/60 bg-background lg:flex">
      <div className="flex h-16 items-center border-b border-border/60 px-5">
        <Logo />
      </div>
      <div className="flex-1 overflow-y-auto p-3">
        <SidebarNav />
      </div>
    </aside>
  );
}
