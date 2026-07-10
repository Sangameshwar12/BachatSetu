import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";

export default function AdminTenantsLoading() {
  return (
    <PageContainer title="Tenant Management">
      <Skeleton className="h-8 w-48 rounded-lg" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }, (_, index) => (
          <Skeleton key={index} className="h-40 rounded-2xl" />
        ))}
      </div>
    </PageContainer>
  );
}
