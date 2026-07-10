import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";

export default function ReceiptsLoading() {
  return (
    <PageContainer title="Receipts">
      <Skeleton className="h-64 rounded-2xl" />
    </PageContainer>
  );
}
