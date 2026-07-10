package in.bachatsetu.backend.admin.application.service;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformUsersUseCase;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformUserRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSearchCriteria;
import java.util.Objects;

/** Searches platform users across every tenant, applying every optional filter, then paginates and sorts. */
public final class ListPlatformUsersApplicationService implements ListPlatformUsersUseCase {

    private final PlatformUserRepository repository;
    private final TransactionPort transaction;
    private final AdminApplicationMapper mapper;

    public ListPlatformUsersApplicationService(
            PlatformUserRepository repository, TransactionPort transaction, AdminApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PlatformPage<PlatformUserResult> execute(PlatformUserSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "search criteria must not be null");
        return transaction.execute(() -> {
            PlatformPage<PlatformUserSummary> page = repository.search(criteria);
            return mapper.toResultPage(page);
        });
    }
}
