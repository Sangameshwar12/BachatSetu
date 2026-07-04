package in.bachatsetu.backend.draw.domain.event;

import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuctionBidSubmitted(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId bidId,
        AggregateId memberId,
        BidAmount amount,
        Instant occurredAt) implements DomainEvent {

    public AuctionBidSubmitted {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(bidId, "bidId must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
