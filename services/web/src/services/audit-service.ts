import { apiClient } from "@/services/api-client";
import type { AuditEntryResponse, AuditSearchParams } from "@/types/audit";
import type { Page } from "@/types/pagination";

/**
 * `GET /api/v1/audit` — searches audit entries within the authenticated caller's own tenant only.
 * There is no cross-tenant, platform-wide audit search endpoint (see Sprint FE-5 report).
 */
export async function searchAudit(params: AuditSearchParams): Promise<Page<AuditEntryResponse>> {
  const { data } = await apiClient.get<Page<AuditEntryResponse>>("/api/v1/audit", { params });
  return data;
}
