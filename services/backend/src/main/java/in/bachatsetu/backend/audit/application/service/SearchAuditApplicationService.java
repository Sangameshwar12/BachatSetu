package in.bachatsetu.backend.audit.application.service;

import in.bachatsetu.backend.audit.application.mapper.AuditApplicationMapper;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.application.usecase.SearchAuditUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import in.bachatsetu.backend.audit.domain.port.AuditSearchCriteria;
import java.util.Objects;

/** Searches audit entries within one tenant, applying every optional filter, then paginates and sorts. */
public final class SearchAuditApplicationService implements SearchAuditUseCase {

    private final AuditRepository repository;
    private final TransactionPort transaction;
    private final AuditApplicationMapper mapper;

    public SearchAuditApplicationService(
            AuditRepository repository, TransactionPort transaction, AuditApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public AuditPage<AuditEntryResult> execute(AuditSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "search criteria must not be null");
        return transaction.execute(() -> {
            AuditPage<AuditEntry> page = repository.search(criteria);
            return mapper.toResultPage(page);
        });
    }
}
