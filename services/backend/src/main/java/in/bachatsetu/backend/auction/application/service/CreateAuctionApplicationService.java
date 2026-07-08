package in.bachatsetu.backend.auction.application.service;

import in.bachatsetu.backend.auction.application.command.CreateAuctionCommand;
import in.bachatsetu.backend.auction.application.mapper.AuctionApplicationMapper;
import in.bachatsetu.backend.auction.application.port.ClockPort;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.auction.application.usecase.CreateAuctionUseCase;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates auction scheduling, enforcing group-owner authorization first.
 *
 * <p>Constructs the underlying {@link Draw} aggregate via {@link Draw#schedule} directly rather than the
 * pre-existing {@code DrawFactory}, and immediately calls {@link Draw#open} in the same operation, so that a
 * created auction is returned already open and ready to accept bids — the Auction REST surface exposes no
 * separate "conduct"/"open" endpoint (only the six endpoints this sprint specifies), so an auction that
 * could only ever be created in {@code SCHEDULED} status would have no way to begin accepting bids. Calling
 * {@code Draw.schedule} directly, with one {@link ClockPort#now()} reading reused for both
 * {@code scheduledAt} and the schedule call's internal {@code createdAt}, avoids the same
 * independent-clock race documented for {@code CreateNotificationApplicationService}
 * (see {@code docs/application/notification-application.md}): {@code DrawFactory} would otherwise derive
 * {@code createdAt} from its own separately injected {@code Clock}, which cannot be guaranteed to land at or
 * after a {@code scheduledAt} of "now" captured beforehand by this service.
 */
public final class CreateAuctionApplicationService implements CreateAuctionUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final DrawAuthorizationService authorization;
    private final AuctionApplicationSupport support;

    public CreateAuctionApplicationService(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            AuctionApplicationMapper mapper,
            DrawAuthorizationService authorization) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.authorization = Objects.requireNonNull(authorization, "authorization must not be null");
        this.support = new AuctionApplicationSupport(
                Objects.requireNonNull(repository, "repository must not be null"), groupRepository, eventPublisher, mapper);
    }

    @Override
    public AuctionResult execute(CreateAuctionCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        return transaction.execute(() -> create(command));
    }

    private AuctionResult create(CreateAuctionCommand command) {
        SavingsGroup group = support.requireOwningGroup(command.tenantId(), command.groupId());
        authorization.requireOwner(group, command.actorId());
        Instant now = clock.now();
        Draw auction = Draw.schedule(
                AggregateId.newId(),
                command.tenantId(),
                command.groupId(),
                command.cycleId(),
                command.number(),
                DrawType.AUCTION,
                now,
                command.actorId(),
                now);
        auction.open(command.actorId(), now);
        return support.saveAndPublish(auction);
    }
}
