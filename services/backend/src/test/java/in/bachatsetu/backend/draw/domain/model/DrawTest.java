package in.bachatsetu.backend.draw.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DrawTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");

    @Test
    void keepsTheHighestAuctionDiscountAsLeadingBid() {
        AggregateId actorId = AggregateId.newId();
        Draw draw = Draw.schedule(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new DrawNumber(1),
                DrawType.AUCTION,
                NOW.plusSeconds(10),
                actorId,
                NOW);
        draw.open(actorId, NOW.plusSeconds(10));

        AuctionBid first = draw.submitBid(
                AggregateId.newId(), new BidAmount(Money.inr(10_000)), actorId, NOW.plusSeconds(20));
        AuctionBid lower = draw.submitBid(
                AggregateId.newId(), new BidAmount(Money.inr(8_000)), actorId, NOW.plusSeconds(30));
        AuctionBid higher = draw.submitBid(
                AggregateId.newId(), new BidAmount(Money.inr(12_000)), actorId, NOW.plusSeconds(40));

        assertThat(first.status()).isEqualTo(BidStatus.OUTBID);
        assertThat(lower.status()).isEqualTo(BidStatus.OUTBID);
        assertThat(higher.status()).isEqualTo(BidStatus.LEADING);
    }
}
