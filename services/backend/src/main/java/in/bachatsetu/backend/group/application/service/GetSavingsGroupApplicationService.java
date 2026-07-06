package in.bachatsetu.backend.group.application.service;

import in.bachatsetu.backend.group.application.exception.SavingsGroupNotFoundException;
import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.usecase.GetSavingsGroupUseCase;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped Savings Group aggregate. */
public final class GetSavingsGroupApplicationService implements GetSavingsGroupUseCase {

    private final SavingsGroupRepository repository;
    private final TransactionPort transaction;
    private final SavingsGroupApplicationMapper mapper;

    public GetSavingsGroupApplicationService(
            SavingsGroupRepository repository,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public SavingsGroupResult execute(AggregateId tenantId, GroupId groupId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        return transaction.execute(() -> {
            SavingsGroup group = repository.findById(tenantId, groupId)
                    .orElseThrow(() -> new SavingsGroupNotFoundException("savings group does not exist"));
            return mapper.toResult(group);
        });
    }
}
