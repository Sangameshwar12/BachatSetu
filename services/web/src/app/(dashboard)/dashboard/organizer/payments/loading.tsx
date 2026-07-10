import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";

export default function OrganizerPaymentsLoading() {
  return (
    <PageContainer title="Payments">
      <Skeleton className="h-64 rounded-2xl" />
    </PageContainer>
  );
}
