package in.bachatsetu.backend.draw.application.service;

import in.bachatsetu.backend.draw.application.command.CloseDrawCommand;
import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.application.usecase.CloseDrawUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import java.util.Objects;

/** Loads a draw, enforces group-owner authorization, and delegates closing it with its winner to the aggregate. */
public final class CloseDrawApplicationService implements CloseDrawUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final DrawAuthorizationService authorization;
    private final DrawApplicationSupport support;

    public CloseDrawApplicationService(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            DrawApplicationMapper mapper,
            DrawAuthorizationService authorization) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.authorization = Objects.requireNonNull(authorization, "authorization must not be null");
        this.support = new DrawApplicationSupport(repository, groupRepository, eventPublisher, mapper);
    }

    @Override
    public DrawResult execute(CloseDrawCommand command) {
        Objects.requireNonNull(command, "close command must not be null");
        return transaction.execute(() -> {
            Draw draw = support.requireDraw(command.tenantId(), command.drawId());
            SavingsGroup group = support.requireOwningGroup(draw.tenantId(), draw.groupId());
            authorization.requireOwner(group, command.actorId());
            draw.complete(command.winnerId(), command.actorId(), clock.now());
            return support.saveAndPublish(draw);
        });
    }
}
