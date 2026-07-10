import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";

export default function InviteMembersLoading() {
  return (
    <PageContainer title="Invite members">
      <Skeleton className="h-80 rounded-2xl" />
    </PageContainer>
  );
}
