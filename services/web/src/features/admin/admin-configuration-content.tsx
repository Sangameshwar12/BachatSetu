"use client";

import { PageContainer } from "@/components/dashboard/page-container";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ConfigFeatureFlagsTab } from "@/features/admin/config-feature-flags-tab";
import { ConfigGeneralTab } from "@/features/admin/config-general-tab";
import { ConfigMaintenanceTab } from "@/features/admin/config-maintenance-tab";
import { ConfigSystemLimitsTab } from "@/features/admin/config-system-limits-tab";

export function AdminConfigurationContent() {
  return (
    <PageContainer title="Platform Configuration" description="Platform-wide settings, maintenance mode, feature flags, and system limits.">
      <Tabs defaultValue="general">
        <TabsList>
          <TabsTrigger value="general">General</TabsTrigger>
          <TabsTrigger value="maintenance">Maintenance mode</TabsTrigger>
          <TabsTrigger value="flags">Feature flags</TabsTrigger>
          <TabsTrigger value="limits">System limits</TabsTrigger>
        </TabsList>

        <TabsContent value="general" className="pt-4">
          <ConfigGeneralTab />
        </TabsContent>
        <TabsContent value="maintenance" className="pt-4">
          <ConfigMaintenanceTab />
        </TabsContent>
        <TabsContent value="flags" className="pt-4">
          <ConfigFeatureFlagsTab />
        </TabsContent>
        <TabsContent value="limits" className="pt-4">
          <ConfigSystemLimitsTab />
        </TabsContent>
      </Tabs>
    </PageContainer>
  );
}
