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
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped Savings Group aggregate. */
public final class GetSavingsGroupApplicationService implements GetSavingsGroupUseCase {

    private static final String FALLBACK_ORGANIZER_NAME = "Group organizer";

    private final SavingsGroupRepository repository;
    private final TransactionPort transaction;
    private final SavingsGroupApplicationMapper mapper;
    private final UserRepository userRepository;

    public GetSavingsGroupApplicationService(
            SavingsGroupRepository repository,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper,
            UserRepository userRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public SavingsGroupResult execute(AggregateId tenantId, GroupId groupId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        return transaction.execute(() -> {
            SavingsGroup group = repository.findById(tenantId, groupId)
                    .orElseThrow(() -> new SavingsGroupNotFoundException("savings group does not exist"));
            String organizerName = userRepository.findById(group.ownerId().value())
                    .map(UserProfile::name)
                    .map(PersonName::displayName)
                    .orElse(FALLBACK_ORGANIZER_NAME);
            return mapper.toResult(group, organizerName);
        });
    }
}
