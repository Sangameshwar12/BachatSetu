/** Mirrors `receipt.interfaces.rest.dto.ReceiptSummaryResponse` exactly. */
export interface ReceiptSummaryResponse {
  receiptId: string;
  number: string;
  totalAmountPaise: number;
  currencyCode: string;
  status: string;
  generatedAt: string;
}

export interface ReceiptLineResponse {
  lineId: string;
  description: string;
  amountPaise: number;
}

/** Mirrors `receipt.interfaces.rest.dto.ReceiptResponse` exactly. */
export interface ReceiptResponse {
  receiptId: string;
  tenantId: string;
  paymentId: string;
  memberId: string;
  number: string;
  lines: ReceiptLineResponse[];
  totalAmountPaise: number;
  currencyCode: string;
  status: string;
  cancellationReason: string | null;
  generatedAt: string;
  updatedAt: string;
  version: number;
}
