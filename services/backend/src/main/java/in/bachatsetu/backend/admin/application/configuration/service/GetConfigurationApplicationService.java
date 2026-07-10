package in.bachatsetu.backend.admin.application.configuration.service;

import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetConfigurationUseCase;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import java.util.Objects;

public final class GetConfigurationApplicationService implements GetConfigurationUseCase {

    private final PlatformConfigurationRepository repository;
    private final TransactionPort transaction;
    private final PlatformConfigApplicationMapper mapper;

    public GetConfigurationApplicationService(
            PlatformConfigurationRepository repository, TransactionPort transaction,
            PlatformConfigApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PlatformConfigurationResult execute() {
        return transaction.execute(() -> mapper.toResult(repository.find()));
    }
}
