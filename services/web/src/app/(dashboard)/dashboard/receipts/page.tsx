import type { Metadata } from "next";

import { ReceiptsContent } from "@/features/receipts/receipts-content";

export const metadata: Metadata = { title: "Receipts" };

export default function ReceiptsPage() {
  return <ReceiptsContent />;
}
