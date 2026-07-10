import { apiClient } from "@/services/api-client";

/**
 * `GET /api/v1/receipts/{receiptId}/pdf` — downloads a receipt as a PDF blob. There is no
 * member-scoped receipt *list* endpoint (see Sprint FE-3 report), so this is only reachable once
 * a `receiptId` is already known.
 */
export async function downloadReceiptPdf(receiptId: string): Promise<Blob> {
  const { data } = await apiClient.get(`/api/v1/receipts/${receiptId}/pdf`, {
    responseType: "blob",
  });
  return data;
}
