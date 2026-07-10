"use client";

import { PageContainer } from "@/components/dashboard/page-container";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AnalyticsGroupsTab } from "@/features/admin/analytics-groups-tab";
import { AnalyticsNotificationsTab } from "@/features/admin/analytics-notifications-tab";
import { AnalyticsOverviewTab } from "@/features/admin/analytics-overview-tab";
import { AnalyticsPaymentsTab } from "@/features/admin/analytics-payments-tab";
import { AnalyticsStorageTab } from "@/features/admin/analytics-storage-tab";
import { AnalyticsUsersTab } from "@/features/admin/analytics-users-tab";

export function AdminAnalyticsContent() {
  return (
    <PageContainer title="Analytics" description="Platform-wide analytics, computed on demand.">
      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="payments">Payments</TabsTrigger>
          <TabsTrigger value="groups">Groups</TabsTrigger>
          <TabsTrigger value="users">Users</TabsTrigger>
          <TabsTrigger value="notifications">Notifications</TabsTrigger>
          <TabsTrigger value="storage">Storage</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="pt-4">
          <AnalyticsOverviewTab />
        </TabsContent>
        <TabsContent value="payments" className="pt-4">
          <AnalyticsPaymentsTab />
        </TabsContent>
        <TabsContent value="groups" className="pt-4">
          <AnalyticsGroupsTab />
        </TabsContent>
        <TabsContent value="users" className="pt-4">
          <AnalyticsUsersTab />
        </TabsContent>
        <TabsContent value="notifications" className="pt-4">
          <AnalyticsNotificationsTab />
        </TabsContent>
        <TabsContent value="storage" className="pt-4">
          <AnalyticsStorageTab />
        </TabsContent>
      </Tabs>
    </PageContainer>
  );
}
