package in.bachatsetu.backend.audit.application.usecase;

import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.domain.port.AuditSearchCriteria;

/** Searches audit entries by any combination of tenant, actor, module, event type, and date range. */
@FunctionalInterface
public interface SearchAuditUseCase {

    AuditPage<AuditEntryResult> execute(AuditSearchCriteria criteria);
}
