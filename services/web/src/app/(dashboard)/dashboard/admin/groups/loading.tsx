import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";

export default function AdminGroupsLoading() {
  return (
    <PageContainer title="Group Management">
      <Skeleton className="h-8 w-full max-w-2xl rounded-lg" />
      <div className="flex flex-col gap-2">
        {Array.from({ length: 6 }, (_, index) => (
          <Skeleton key={index} className="h-12 rounded-lg" />
        ))}
      </div>
    </PageContainer>
  );
}
