package in.bachatsetu.backend.group.application.service;

import in.bachatsetu.backend.group.application.command.ActivateGroupCommand;
import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.security.GroupAuthorizationService;
import in.bachatsetu.backend.group.application.usecase.ActivateGroupUseCase;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import java.util.Objects;

/** Loads a group, enforces owner authorization, and delegates activation to the aggregate. */
public final class ActivateGroupApplicationService implements ActivateGroupUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final GroupAuthorizationService authorization;
    private final SavingsGroupApplicationSupport support;

    public ActivateGroupApplicationService(
            SavingsGroupRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper,
            GroupAuthorizationService authorization) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.authorization = Objects.requireNonNull(authorization, "authorization must not be null");
        this.support = new SavingsGroupApplicationSupport(repository, eventPublisher, mapper);
    }

    @Override
    public SavingsGroupResult execute(ActivateGroupCommand command) {
        Objects.requireNonNull(command, "activate command must not be null");
        return transaction.execute(() -> {
            SavingsGroup group = support.requireGroup(command.tenantId(), command.groupId());
            authorization.requireOwner(group, command.actorId());
            group.activate(command.actorId(), clock.now());
            return support.saveAndPublish(group);
        });
    }
}
