/** One row of `GET /api/v1/audit` — always scoped to the caller's own tenant. */
export interface AuditEntryResponse {
  auditId: string;
  tenantId: string | null;
  actorId: string | null;
  eventType: string;
  moduleName: string;
  resourceType: string | null;
  resourceId: string | null;
  action: string;
  description: string;
  ipAddress: string | null;
  userAgent: string | null;
  metadata: string | null;
  createdAt: string;
}

export interface AuditSearchParams {
  actor?: string;
  module?: string;
  event?: string;
  page?: number;
  size?: number;
}
