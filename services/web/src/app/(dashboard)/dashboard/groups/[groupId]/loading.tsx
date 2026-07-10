import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";

export default function GroupDetailsLoading() {
  return (
    <PageContainer title="Group details">
      <Skeleton className="h-9 w-full max-w-md rounded-lg" />
      <Skeleton className="h-96 rounded-2xl" />
    </PageContainer>
  );
}
