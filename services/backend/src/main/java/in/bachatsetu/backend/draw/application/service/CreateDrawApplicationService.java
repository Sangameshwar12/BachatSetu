package in.bachatsetu.backend.draw.application.service;

import in.bachatsetu.backend.draw.application.command.CreateDrawCommand;
import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.application.usecase.CreateDrawUseCase;
import in.bachatsetu.backend.draw.domain.factory.DrawFactory;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import java.util.Objects;

/** Coordinates draw scheduling, enforcing group-owner authorization first. */
public final class CreateDrawApplicationService implements CreateDrawUseCase {

    private final DrawFactory drawFactory;
    private final TransactionPort transaction;
    private final DrawAuthorizationService authorization;
    private final DrawApplicationSupport support;

    public CreateDrawApplicationService(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DrawFactory drawFactory,
            DomainEventPublisherPort eventPublisher,
            TransactionPort transaction,
            DrawApplicationMapper mapper,
            DrawAuthorizationService authorization) {
        this.drawFactory = Objects.requireNonNull(drawFactory, "draw factory must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.authorization = Objects.requireNonNull(authorization, "authorization must not be null");
        this.support = new DrawApplicationSupport(repository, groupRepository, eventPublisher, mapper);
    }

    @Override
    public DrawResult execute(CreateDrawCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        return transaction.execute(() -> create(command));
    }

    private DrawResult create(CreateDrawCommand command) {
        SavingsGroup group = support.requireOwningGroup(command.tenantId(), command.groupId());
        authorization.requireOwner(group, command.actorId());
        Draw draw = drawFactory.schedule(
                command.tenantId(),
                command.groupId(),
                command.cycleId(),
                command.number(),
                command.type(),
                command.scheduledAt(),
                command.actorId());
        return support.saveAndPublish(draw);
    }
}
