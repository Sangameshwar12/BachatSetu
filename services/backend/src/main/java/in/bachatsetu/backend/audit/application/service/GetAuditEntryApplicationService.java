package in.bachatsetu.backend.audit.application.service;

import in.bachatsetu.backend.audit.application.exception.AuditEntryNotFoundException;
import in.bachatsetu.backend.audit.application.mapper.AuditApplicationMapper;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.application.usecase.GetAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves and maps one tenant-scoped {@link AuditEntry} aggregate. */
public final class GetAuditEntryApplicationService implements GetAuditEntryUseCase {

    private final AuditRepository repository;
    private final TransactionPort transaction;
    private final AuditApplicationMapper mapper;

    public GetAuditEntryApplicationService(
            AuditRepository repository, TransactionPort transaction, AuditApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public AuditEntryResult execute(AggregateId tenantId, AggregateId auditId) {
        Objects.requireNonNull(auditId, "audit id must not be null");
        return transaction.execute(() -> {
            AuditEntry entry = repository.findById(tenantId, auditId)
                    .orElseThrow(() -> new AuditEntryNotFoundException("audit entry does not exist"));
            return mapper.toResult(entry);
        });
    }
}
