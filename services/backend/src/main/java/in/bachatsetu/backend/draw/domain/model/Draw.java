package in.bachatsetu.backend.draw.domain.model;

import in.bachatsetu.backend.draw.domain.event.AuctionBidSubmitted;
import in.bachatsetu.backend.draw.domain.event.DrawCompleted;
import in.bachatsetu.backend.draw.domain.event.DrawScheduled;
import in.bachatsetu.backend.draw.domain.exception.DuplicateBidException;
import in.bachatsetu.backend.draw.domain.exception.InvalidDrawStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Draw extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final AggregateId groupId;
    private final AggregateId cycleId;
    private final DrawNumber number;
    private final DrawType type;
    private final Instant scheduledAt;
    private DrawStatus status;
    private final List<AuctionBid> bids;
    private AggregateId winnerMemberId;

    public Draw(
            AggregateId id,
            AggregateId tenantId,
            AggregateId groupId,
            AggregateId cycleId,
            DrawNumber number,
            DrawType type,
            Instant scheduledAt,
            DrawStatus status,
            List<AuctionBid> bids,
            AggregateId winnerMemberId,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.cycleId = Objects.requireNonNull(cycleId, "cycleId must not be null");
        this.number = Objects.requireNonNull(number, "number must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.bids = new ArrayList<>(Objects.requireNonNull(bids, "bids must not be null"));
        this.winnerMemberId = winnerMemberId;
    }

    public static Draw schedule(
            AggregateId id,
            AggregateId tenantId,
            AggregateId groupId,
            AggregateId cycleId,
            DrawNumber number,
            DrawType type,
            Instant scheduledAt,
            AggregateId actorId,
            Instant createdAt) {
        if (scheduledAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("scheduledAt must not be in the past");
        }
        Draw draw = new Draw(
                id,
                tenantId,
                groupId,
                cycleId,
                number,
                type,
                scheduledAt,
                DrawStatus.SCHEDULED,
                List.of(),
                null,
                AuditInfo.createdBy(actorId, createdAt),
                0);
        draw.registerEvent(new DrawScheduled(
                UUID.randomUUID(), id, groupId, cycleId, number, scheduledAt, createdAt));
        return draw;
    }

    public void open(AggregateId actorId, Instant openedAt) {
        if (status != DrawStatus.SCHEDULED || openedAt.isBefore(scheduledAt)) {
            throw new InvalidDrawStateException("draw cannot be opened");
        }
        status = DrawStatus.OPEN;
        markChanged(actorId, openedAt);
    }

    public AuctionBid submitBid(
            AggregateId memberId,
            BidAmount amount,
            AggregateId actorId,
            Instant submittedAt) {
        if (type != DrawType.AUCTION || status != DrawStatus.OPEN) {
            throw new InvalidDrawStateException("draw is not accepting auction bids");
        }
        boolean duplicate = bids.stream().anyMatch(bid ->
                bid.memberId().equals(memberId)
                        && bid.status() != BidStatus.WITHDRAWN
                        && bid.status() != BidStatus.REJECTED);
        if (duplicate) {
            throw new DuplicateBidException("member already has an active bid");
        }
        AuctionBid currentLeader = bids.stream()
                .filter(bid -> bid.status() == BidStatus.LEADING)
                .findFirst()
                .orElse(null);
        AuctionBid bid = new AuctionBid(
                AggregateId.newId(), memberId, amount, submittedAt, BidStatus.SUBMITTED);
        if (currentLeader == null) {
            bid.markLeading();
        } else if (amount.compareTo(currentLeader.amount()) > 0) {
            currentLeader.markOutbid();
            bid.markLeading();
        } else {
            bid.markOutbid();
        }
        bids.add(bid);
        markChanged(actorId, submittedAt);
        registerEvent(new AuctionBidSubmitted(
                UUID.randomUUID(), id(), bid.id(), memberId, amount, submittedAt));
        return bid;
    }

    public void complete(AggregateId winnerId, AggregateId actorId, Instant completedAt) {
        if (status != DrawStatus.OPEN) {
            throw new InvalidDrawStateException("only an open draw can be completed");
        }
        if (type == DrawType.AUCTION) {
            AuctionBid winningBid = bids.stream()
                    .filter(bid -> bid.memberId().equals(winnerId) && bid.status() == BidStatus.LEADING)
                    .findFirst()
                    .orElseThrow(() -> new InvalidDrawStateException("winner must own the leading bid"));
            winningBid.accept();
        }
        winnerMemberId = Objects.requireNonNull(winnerId, "winnerId must not be null");
        status = DrawStatus.COMPLETED;
        markChanged(actorId, completedAt);
        registerEvent(new DrawCompleted(UUID.randomUUID(), id(), winnerId, completedAt));
    }

    public AggregateId tenantId() { return tenantId; }
    public AggregateId groupId() { return groupId; }
    public AggregateId cycleId() { return cycleId; }
    public DrawNumber number() { return number; }
    public DrawType type() { return type; }
    public Instant scheduledAt() { return scheduledAt; }
    public DrawStatus status() { return status; }
    public List<AuctionBid> bids() { return List.copyOf(bids); }
    public AggregateId winnerMemberId() { return winnerMemberId; }
}
