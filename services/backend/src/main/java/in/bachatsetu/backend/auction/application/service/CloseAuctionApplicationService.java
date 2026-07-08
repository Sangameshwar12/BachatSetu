package in.bachatsetu.backend.auction.application.service;

import in.bachatsetu.backend.auction.application.command.CloseAuctionCommand;
import in.bachatsetu.backend.auction.application.mapper.AuctionApplicationMapper;
import in.bachatsetu.backend.auction.application.port.ClockPort;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.auction.application.usecase.CloseAuctionUseCase;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import java.util.Objects;

/**
 * Loads an auction, enforces group-owner authorization, and delegates closing it with its winner to the
 * aggregate. {@link Draw#complete} already enforces every immutability and winner-selection rule (only an
 * open auction can be closed, and only once); this service adds no new business rule on top of it. Closing
 * publishes the aggregate's existing {@code DrawCompleted} event, which a separate listener reacts to in
 * order to notify the winner — see {@code docs/application/auction-application.md}.
 */
public final class CloseAuctionApplicationService implements CloseAuctionUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final DrawAuthorizationService authorization;
    private final AuctionApplicationSupport support;

    public CloseAuctionApplicationService(
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
    public AuctionResult execute(CloseAuctionCommand command) {
        Objects.requireNonNull(command, "close command must not be null");
        return transaction.execute(() -> {
            Draw auction = support.requireAuction(command.tenantId(), command.auctionId());
            SavingsGroup group = support.requireOwningGroup(auction.tenantId(), auction.groupId());
            authorization.requireOwner(group, command.actorId());
            auction.complete(command.winnerId(), command.actorId(), clock.now());
            return support.saveAndPublish(auction);
        });
    }
}
