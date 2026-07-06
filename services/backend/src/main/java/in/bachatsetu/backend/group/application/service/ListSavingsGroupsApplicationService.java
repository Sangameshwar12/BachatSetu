package in.bachatsetu.backend.group.application.service;

import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.group.application.usecase.ListSavingsGroupsUseCase;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/** Lists tenant-scoped groups as compact immutable query models. */
public final class ListSavingsGroupsApplicationService implements ListSavingsGroupsUseCase {

    private final SavingsGroupRepository repository;
    private final TransactionPort transaction;
    private final SavingsGroupApplicationMapper mapper;

    public ListSavingsGroupsApplicationService(
            SavingsGroupRepository repository,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<SavingsGroupSummary> execute(AggregateId tenantId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        return transaction.execute(() -> repository.findAll(tenantId).stream()
                .map(mapper::toSummary)
                .toList());
    }
}
