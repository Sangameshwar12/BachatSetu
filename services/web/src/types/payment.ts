/** Mirrors `payment.interfaces.rest.dto.PaymentSummaryResponse` exactly. */
export interface PaymentSummaryResponse {
  paymentId: string;
  reference: string;
  amountPaise: number;
  currencyCode: string;
  method: string;
  status: string;
  createdAt: string;
}

export interface PaymentAttemptResponse {
  attemptId: string;
  status: string;
  providerReference: string | null;
  failureCode: string | null;
  createdAt: string;
}

/** Mirrors `payment.interfaces.rest.dto.PaymentResponse` exactly. */
export interface PaymentResponse {
  paymentId: string;
  tenantId: string;
  groupId: string;
  memberId: string;
  reference: string;
  amountPaise: number;
  currencyCode: string;
  method: string;
  status: string;
  reconciliationStatus: string;
  attempts: PaymentAttemptResponse[];
  createdAt: string;
  updatedAt: string;
  version: number;
}
