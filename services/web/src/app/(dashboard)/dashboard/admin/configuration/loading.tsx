import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";

export default function AdminConfigurationLoading() {
  return (
    <PageContainer title="Platform Configuration">
      <Skeleton className="h-8 w-96 rounded-lg" />
      <div className="flex max-w-xl flex-col gap-3">
        {Array.from({ length: 5 }, (_, index) => (
          <Skeleton key={index} className="h-10 rounded-lg" />
        ))}
      </div>
    </PageContainer>
  );
}
