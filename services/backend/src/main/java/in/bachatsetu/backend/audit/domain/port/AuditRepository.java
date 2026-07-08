package in.bachatsetu.backend.audit.domain.port;

import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface AuditRepository {

    /** {@code tenantId} may be {@code null} to look up a tenant-less (system) entry. */
    Optional<AuditEntry> findById(AggregateId tenantId, AggregateId auditId);

    AuditPage<AuditEntry> search(AuditSearchCriteria criteria);

    void save(AuditEntry entry);
}
