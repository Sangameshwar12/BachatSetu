package in.bachatsetu.backend.auction.application.service;

import in.bachatsetu.backend.auction.application.exception.AuctionAccessDeniedException;
import in.bachatsetu.backend.auction.application.exception.AuctionNotFoundException;
import in.bachatsetu.backend.auction.application.mapper.AuctionApplicationMapper;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Shared persistence and event-publication mechanics for Auction command services. */
final class AuctionApplicationSupport {

    private final DrawRepository repository;
    private final SavingsGroupRepository groupRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final AuctionApplicationMapper mapper;

    AuctionApplicationSupport(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            AuctionApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Loads a tenant-scoped draw and confirms it is an auction, so that the Auction REST surface never
     * exposes a RANDOM or FIXED_ROTATION draw under an {@code /api/v1/auctions} path.
     */
    Draw requireAuction(AggregateId tenantId, AggregateId auctionId) {
        Draw draw = repository.findById(tenantId, auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("auction does not exist"));
        if (draw.type() != DrawType.AUCTION) {
            throw new AuctionNotFoundException("auction does not exist");
        }
        return draw;
    }

    SavingsGroup requireOwningGroup(AggregateId tenantId, AggregateId groupId) {
        return groupRepository.findById(tenantId, new GroupId(groupId))
                .orElseThrow(() -> new AuctionAccessDeniedException("only the group owner may perform this operation"));
    }

    AuctionResult saveAndPublish(Draw auction) {
        repository.save(auction);
        eventPublisher.publish(auction.pullDomainEvents());
        return mapper.toResult(auction);
    }
}
