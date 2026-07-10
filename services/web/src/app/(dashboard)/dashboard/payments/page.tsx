import type { Metadata } from "next";

import { PaymentsContent } from "@/features/payments/payments-content";

export const metadata: Metadata = { title: "Payments" };

export default function PaymentsPage() {
  return <PaymentsContent />;
}
