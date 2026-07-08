package in.bachatsetu.backend.auction.application.service;

import in.bachatsetu.backend.auction.application.command.PlaceBidCommand;
import in.bachatsetu.backend.auction.application.exception.InvalidBidAmountException;
import in.bachatsetu.backend.auction.application.exception.MemberNotEligibleException;
import in.bachatsetu.backend.auction.application.mapper.AuctionApplicationMapper;
import in.bachatsetu.backend.auction.application.port.ClockPort;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.application.usecase.PlaceBidUseCase;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import in.bachatsetu.backend.draw.domain.model.AuctionBid;
import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;

/**
 * Coordinates bid placement, enforcing two cross-aggregate rules {@link Draw#submitBid} cannot enforce
 * itself because they depend on the {@link SavingsGroup} aggregate rather than the auction's own state:
 * bidder eligibility (must be an active group member) and bid range (must not exceed the group's
 * contribution amount). Every rule {@code Draw#submitBid} already owns — no duplicate bids, auction must be
 * open, highest discount leads — is enforced unchanged by the aggregate itself.
 */
public final class PlaceBidApplicationService implements PlaceBidUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final AuctionApplicationMapper mapper;
    private final AuctionApplicationSupport support;

    public PlaceBidApplicationService(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            AuctionApplicationMapper mapper) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.support = new AuctionApplicationSupport(
                Objects.requireNonNull(repository, "repository must not be null"), groupRepository, eventPublisher, mapper);
    }

    @Override
    public AuctionBidResult execute(PlaceBidCommand command) {
        Objects.requireNonNull(command, "place bid command must not be null");
        return transaction.execute(() -> placeBid(command));
    }

    private AuctionBidResult placeBid(PlaceBidCommand command) {
        Draw auction = support.requireAuction(command.tenantId(), command.auctionId());
        SavingsGroup group = support.requireOwningGroup(auction.tenantId(), auction.groupId());
        requireEligibleMember(group, command.memberId());
        requireAmountWithinRange(group, command.discountAmount());
        AuctionBid bid = auction.submitBid(
                command.memberId(), new BidAmount(command.discountAmount()), command.actorId(), clock.now());
        support.saveAndPublish(auction);
        return mapper.toBidResult(bid);
    }

    private void requireEligibleMember(SavingsGroup group, AggregateId memberId) {
        boolean eligible = group.members().stream()
                .anyMatch(member -> member.memberId().equals(memberId) && member.isActive());
        if (!eligible) {
            throw new MemberNotEligibleException("member is not an active participant of this group");
        }
    }

    private void requireAmountWithinRange(SavingsGroup group, Money discountAmount) {
        if (discountAmount.compareTo(group.contributionAmount().value()) > 0) {
            throw new InvalidBidAmountException("bid discount must not exceed the group's contribution amount");
        }
    }
}
