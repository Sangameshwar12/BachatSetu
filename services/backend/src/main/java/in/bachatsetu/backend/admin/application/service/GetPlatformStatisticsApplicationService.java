package in.bachatsetu.backend.admin.application.service;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;
import in.bachatsetu.backend.admin.application.usecase.GetPlatformStatisticsUseCase;
import in.bachatsetu.backend.admin.domain.model.PlatformStatistics;
import in.bachatsetu.backend.admin.domain.port.PlatformStatisticsRepository;
import java.util.Objects;

/** Computes and maps platform-wide totals. */
public final class GetPlatformStatisticsApplicationService implements GetPlatformStatisticsUseCase {

    private final PlatformStatisticsRepository repository;
    private final TransactionPort transaction;
    private final AdminApplicationMapper mapper;

    public GetPlatformStatisticsApplicationService(
            PlatformStatisticsRepository repository, TransactionPort transaction, AdminApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PlatformStatisticsResult execute() {
        return transaction.execute(() -> {
            PlatformStatistics statistics = repository.compute();
            return mapper.toResult(statistics);
        });
    }
}
