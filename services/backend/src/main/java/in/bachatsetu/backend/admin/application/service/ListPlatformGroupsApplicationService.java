package in.bachatsetu.backend.admin.application.service;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformGroupsUseCase;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import java.util.Objects;

/** Searches savings groups across every tenant, applying every optional filter, then paginates. */
public final class ListPlatformGroupsApplicationService implements ListPlatformGroupsUseCase {

    private final PlatformGroupRepository repository;
    private final TransactionPort transaction;
    private final AdminApplicationMapper mapper;

    public ListPlatformGroupsApplicationService(
            PlatformGroupRepository repository, TransactionPort transaction, AdminApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PlatformPage<PlatformGroupResult> execute(PlatformGroupSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "search criteria must not be null");
        return transaction.execute(() -> {
            PlatformPage<PlatformGroupSummary> page = repository.search(criteria);
            return mapper.toGroupResultPage(page);
        });
    }
}
