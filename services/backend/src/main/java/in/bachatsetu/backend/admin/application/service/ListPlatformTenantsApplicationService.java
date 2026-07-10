package in.bachatsetu.backend.admin.application.service;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformTenantsUseCase;
import in.bachatsetu.backend.admin.domain.model.PlatformTenantSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformPageRequest;
import in.bachatsetu.backend.admin.domain.port.PlatformTenantRepository;
import java.util.Objects;

/** Lists tenants known to the platform, derived from their users, with per-tenant totals. */
public final class ListPlatformTenantsApplicationService implements ListPlatformTenantsUseCase {

    private final PlatformTenantRepository repository;
    private final TransactionPort transaction;
    private final AdminApplicationMapper mapper;

    public ListPlatformTenantsApplicationService(
            PlatformTenantRepository repository, TransactionPort transaction, AdminApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PlatformPage<PlatformTenantResult> execute(PlatformPageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return transaction.execute(() -> {
            PlatformPage<PlatformTenantSummary> page = repository.search(pageRequest);
            return mapper.toTenantResultPage(page);
        });
    }
}
