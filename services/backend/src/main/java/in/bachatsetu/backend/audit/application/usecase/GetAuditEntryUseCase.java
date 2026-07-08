package in.bachatsetu.backend.audit.application.usecase;

import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves one audit entry. {@code tenantId} may be {@code null} to look up a tenant-less (system) entry. */
@FunctionalInterface
public interface GetAuditEntryUseCase {

    AuditEntryResult execute(AggregateId tenantId, AggregateId auditId);
}
