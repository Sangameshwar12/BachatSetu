package in.bachatsetu.backend.group.application.service;

import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.exception.DuplicateGroupCodeException;
import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase;
import in.bachatsetu.backend.group.domain.model.CreatedAt;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import java.util.Objects;

/** Coordinates Savings Group creation without owning business invariants. */
public final class CreateSavingsGroupApplicationService implements CreateSavingsGroupUseCase {

    private final SavingsGroupRepository repository;
    private final GroupCodeGeneratorPort codeGenerator;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final SavingsGroupApplicationSupport support;

    public CreateSavingsGroupApplicationService(
            SavingsGroupRepository repository,
            GroupCodeGeneratorPort codeGenerator,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "code generator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new SavingsGroupApplicationSupport(repository, eventPublisher, mapper);
    }

    @Override
    public SavingsGroupResult execute(CreateSavingsGroupCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        return transaction.execute(() -> create(command));
    }

    private SavingsGroupResult create(CreateSavingsGroupCommand command) {
        GroupId groupId = GroupId.newId();
        GroupCode groupCode = Objects.requireNonNull(
                codeGenerator.generate(groupId), "generated group code must not be null");
        if (repository.existsByGroupCode(command.tenantId(), groupCode)) {
            throw new DuplicateGroupCodeException("generated group code already exists");
        }
        SavingsGroup group = SavingsGroup.create(
                groupId,
                command.tenantId(),
                command.ownerId(),
                groupCode,
                command.name(),
                command.description(),
                command.type(),
                command.rule(),
                new CreatedAt(clock.now()));
        return support.saveAndPublish(group);
    }
}
