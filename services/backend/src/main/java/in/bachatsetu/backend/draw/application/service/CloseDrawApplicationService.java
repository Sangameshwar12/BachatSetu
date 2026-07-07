package in.bachatsetu.backend.draw.application.service;

import in.bachatsetu.backend.draw.application.command.CloseDrawCommand;
import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.usecase.CloseDrawUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import java.util.Objects;

/** Loads a draw and delegates closing (completing) it with its winner to the aggregate. */
public final class CloseDrawApplicationService implements CloseDrawUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final DrawApplicationSupport support;

    public CloseDrawApplicationService(
            DrawRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            DrawApplicationMapper mapper) {
        Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new DrawApplicationSupport(repository, eventPublisher, mapper);
    }

    @Override
    public DrawResult execute(CloseDrawCommand command) {
        Objects.requireNonNull(command, "close command must not be null");
        return transaction.execute(() -> {
            Draw draw = support.requireDraw(command.tenantId(), command.drawId());
            draw.complete(command.winnerId(), command.actorId(), clock.now());
            return support.saveAndPublish(draw);
        });
    }
}
