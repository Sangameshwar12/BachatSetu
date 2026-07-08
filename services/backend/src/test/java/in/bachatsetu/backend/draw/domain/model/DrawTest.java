package in.bachatsetu.backend.draw.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.draw.domain.exception.InvalidDrawStateException;
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

    @Test
    void completesAnAuctionWithItsLeadingBid() {
        AggregateId actorId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        Draw draw = newAuction(actorId);
        draw.open(actorId, NOW.plusSeconds(10));
        AuctionBid bid = draw.submitBid(
                memberId, new BidAmount(Money.inr(10_000)), actorId, NOW.plusSeconds(20));

        draw.complete(memberId, actorId, NOW.plusSeconds(30));

        assertThat(draw.status()).isEqualTo(DrawStatus.COMPLETED);
        assertThat(draw.winnerMemberId()).isEqualTo(memberId);
        assertThat(bid.status()).isEqualTo(BidStatus.ACCEPTED);
        assertThat(draw.domainEvents()).hasSize(3);
    }

    @Test
    void rejectsOpeningBeforeTheScheduledTime() {
        AggregateId actorId = AggregateId.newId();
        Draw draw = newAuction(actorId);

        assertThatThrownBy(() -> draw.open(actorId, NOW.plusSeconds(5)))
                .isInstanceOf(InvalidDrawStateException.class);
    }

    @Test
    void reportsTheWinningBidOnceACompletedAuctionHasOne() {
        AggregateId actorId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        Draw draw = newAuction(actorId);
        draw.open(actorId, NOW.plusSeconds(10));
        draw.submitBid(memberId, new BidAmount(Money.inr(10_000)), actorId, NOW.plusSeconds(20));

        draw.complete(memberId, actorId, NOW.plusSeconds(30));

        assertThat(draw.winner()).isPresent();
        assertThat(draw.winner().orElseThrow().memberId()).isEqualTo(memberId);
        assertThat(draw.winner().orElseThrow().winningAmount()).isEqualTo(new BidAmount(Money.inr(10_000)));
        assertThat(draw.winner().orElseThrow().decidedAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void reportsNoWinnerForAnIncompleteAuction() {
        AggregateId actorId = AggregateId.newId();
        Draw draw = newAuction(actorId);
        draw.open(actorId, NOW.plusSeconds(10));
        draw.submitBid(AggregateId.newId(), new BidAmount(Money.inr(10_000)), actorId, NOW.plusSeconds(20));

        assertThat(draw.winner()).isEmpty();
    }

    @Test
    void reportsNoWinnerForACompletedNonAuctionDraw() {
        AggregateId actorId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        Draw draw = Draw.schedule(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new DrawNumber(1), DrawType.RANDOM, NOW.plusSeconds(10), actorId, NOW);
        draw.open(actorId, NOW.plusSeconds(10));

        draw.complete(memberId, actorId, NOW.plusSeconds(30));

        assertThat(draw.winner()).isEmpty();
    }

    private Draw newAuction(AggregateId actorId) {
        return Draw.schedule(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new DrawNumber(1),
                DrawType.AUCTION,
                NOW.plusSeconds(10),
                actorId,
                NOW);
    }
}
