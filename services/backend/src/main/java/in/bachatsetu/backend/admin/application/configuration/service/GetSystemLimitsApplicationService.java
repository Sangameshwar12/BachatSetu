package in.bachatsetu.backend.admin.application.configuration.service;

import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetSystemLimitsUseCase;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformLimitRepository;
import java.util.List;
import java.util.Objects;

public final class GetSystemLimitsApplicationService implements GetSystemLimitsUseCase {

    private final PlatformLimitRepository repository;
    private final TransactionPort transaction;
    private final PlatformConfigApplicationMapper mapper;

    public GetSystemLimitsApplicationService(
            PlatformLimitRepository repository, TransactionPort transaction, PlatformConfigApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<PlatformLimitResult> execute() {
        return transaction.execute(() -> repository.findAll().stream().map(mapper::toResult).toList());
    }
}
