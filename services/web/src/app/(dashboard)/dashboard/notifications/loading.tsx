import { PageContainer } from "@/components/dashboard/page-container";
import { Skeleton } from "@/components/ui/skeleton";

export default function NotificationsLoading() {
  return (
    <PageContainer title="Notifications">
      <Skeleton className="h-10 w-full max-w-sm rounded-lg" />
      <Skeleton className="h-72 rounded-2xl" />
    </PageContainer>
  );
}
