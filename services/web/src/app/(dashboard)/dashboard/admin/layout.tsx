import { RoleGuard } from "@/components/auth/role-guard";
import { PLATFORM_ADMIN_ROLE } from "@/constants/auth";

export default function AdminSectionLayout({ children }: { children: React.ReactNode }) {
  return <RoleGuard role={PLATFORM_ADMIN_ROLE}>{children}</RoleGuard>;
}
