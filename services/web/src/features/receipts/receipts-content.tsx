"use client";

import { Info, Receipt } from "lucide-react";

import { PageContainer } from "@/components/dashboard/page-container";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { EmptyState } from "@/components/shared/empty-state";

export function ReceiptsContent() {
  return (
    <PageContainer title="Receipts" description="Download receipts for your verified payments.">
      <EmptyState
        icon={Receipt}
        title="No receipts to show yet"
        description="Receipts are generated automatically once a payment is verified. Yours will appear here as soon as one exists."
      />
      <Alert>
        <Info />
        <AlertTitle>A member-scoped receipt list isn&apos;t available yet</AlertTitle>
        <AlertDescription>
          The Receipts API can retrieve and download a specific receipt once its ID is known, but
          there&apos;s no endpoint yet to list every receipt belonging to a member — so this page
          can&apos;t safely show &quot;your&quot; receipts without exposing other members&apos;
          data. This will be wired up once that endpoint ships.
        </AlertDescription>
      </Alert>
    </PageContainer>
  );
}
