package in.bachatsetu.backend.draw.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DrawApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private final DrawApplicationMapper mapper = new DrawApplicationMapper();

    @Test
    void mapsDrawToResultIncludingBids() {
        AggregateId actorId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        Draw draw = newAuction(actorId);
        draw.open(actorId, NOW.plusSeconds(3600));
        draw.submitBid(memberId, new BidAmount(Money.inr(10_000)), actorId, NOW.plusSeconds(3700));

        var result = mapper.toResult(draw);

        assertThat(result.drawId()).isEqualTo(draw.id().value());
        assertThat(result.number()).isEqualTo(1);
        assertThat(result.type()).isEqualTo("AUCTION");
        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.winnerMemberId()).isNull();
        assertThat(result.bids()).singleElement().satisfies(bid -> {
            assertThat(bid.memberId()).isEqualTo(memberId.value());
            assertThat(bid.discountAmountPaise()).isEqualTo(10_000L);
            assertThat(bid.status()).isEqualTo("LEADING");
        });
        assertThatThrownBy(() -> result.bids().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapsCompletedDrawWinnerId() {
        AggregateId actorId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        Draw draw = newAuction(actorId);
        draw.open(actorId, NOW.plusSeconds(3600));
        draw.submitBid(memberId, new BidAmount(Money.inr(10_000)), actorId, NOW.plusSeconds(3700));
        draw.complete(memberId, actorId, NOW.plusSeconds(3800));

        var result = mapper.toResult(draw);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.winnerMemberId()).isEqualTo(memberId.value());
    }

    @Test
    void mapsDrawToSummary() {
        Draw draw = newAuction(AggregateId.newId());

        var summary = mapper.toSummary(draw);

        assertThat(summary.drawId()).isEqualTo(draw.id().value());
        assertThat(summary.status()).isEqualTo("SCHEDULED");
        assertThat(summary.winnerMemberId()).isNull();
    }

    @Test
    void rejectsNullInputs() {
        assertThatThrownBy(() -> mapper.toResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toSummary(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toBidResult(null)).isInstanceOf(NullPointerException.class);
    }

    private Draw newAuction(AggregateId actorId) {
        return Draw.schedule(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new DrawNumber(1),
                DrawType.AUCTION,
                NOW.plusSeconds(3600),
                actorId,
                NOW);
    }
}
