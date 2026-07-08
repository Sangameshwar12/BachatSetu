package in.bachatsetu.backend.auction.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auction.application.exception.AuctionNotFoundException;
import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuctionApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private final AuctionApplicationMapper mapper = new AuctionApplicationMapper();

    @Test
    void mapsAnOpenAuctionToResult() {
        Draw auction = newOpenAuction();

        var result = mapper.toResult(auction);

        assertThat(result.auctionId()).isEqualTo(auction.id().value());
        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.winnerMemberId()).isNull();
        assertThat(result.bids()).isEmpty();
    }

    @Test
    void mapsAnAuctionWithBidsToResult() {
        Draw auction = newOpenAuction();
        AggregateId memberId = AggregateId.newId();
        auction.submitBid(memberId, new BidAmount(Money.inr(10_000)), memberId, NOW.plusSeconds(20));

        var result = mapper.toResult(auction);

        assertThat(result.bids()).singleElement().satisfies(bid -> {
            assertThat(bid.memberId()).isEqualTo(memberId.value());
            assertThat(bid.status()).isEqualTo("LEADING");
        });
    }

    @Test
    void mapsAuctionToSummary() {
        Draw auction = newOpenAuction();

        var summary = mapper.toSummary(auction);

        assertThat(summary.auctionId()).isEqualTo(auction.id().value());
        assertThat(summary.status()).isEqualTo("OPEN");
    }

    @Test
    void mapsAClosedAuctionToWinnerResult() {
        Draw auction = newOpenAuction();
        AggregateId memberId = AggregateId.newId();
        auction.submitBid(memberId, new BidAmount(Money.inr(15_000)), memberId, NOW.plusSeconds(20));
        auction.complete(memberId, memberId, NOW.plusSeconds(30));

        var winner = mapper.toWinnerResult(auction);

        assertThat(winner.auctionId()).isEqualTo(auction.id().value());
        assertThat(winner.memberId()).isEqualTo(memberId.value());
        assertThat(winner.winningDiscountAmountPaise()).isEqualTo(15_000L);
        assertThat(winner.currencyCode()).isEqualTo("INR");
    }

    @Test
    void rejectsWinnerResultForAnAuctionWithNoWinner() {
        Draw auction = newOpenAuction();

        assertThatThrownBy(() -> mapper.toWinnerResult(auction))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    void rejectsNullInputs() {
        assertThatThrownBy(() -> mapper.toResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toSummary(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toWinnerResult(null)).isInstanceOf(NullPointerException.class);
    }

    private Draw newOpenAuction() {
        AggregateId actorId = AggregateId.newId();
        Draw auction = Draw.schedule(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new DrawNumber(1), DrawType.AUCTION, NOW, actorId, NOW);
        auction.open(actorId, NOW);
        return auction;
    }
}
