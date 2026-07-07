package in.bachatsetu.backend.receipt.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceiptApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    private final ReceiptApplicationMapper mapper = new ReceiptApplicationMapper();

    @Test
    void mapsReceiptToResultIncludingLines() {
        Receipt receipt = newReceipt();

        var result = mapper.toResult(receipt);

        assertThat(result.receiptId()).isEqualTo(receipt.id().value());
        assertThat(result.number()).isEqualTo("RCT/20260707/00000001");
        assertThat(result.status()).isEqualTo("GENERATED");
        assertThat(result.totalAmountPaise()).isEqualTo(600_000L);
        assertThat(result.currencyCode()).isEqualTo("INR");
        assertThat(result.cancellationReason()).isNull();
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().getFirst().type()).isEqualTo("CONTRIBUTION");
        assertThatThrownBy(() -> result.lines().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapsCancelledReceiptWithReason() {
        Receipt receipt = newReceipt();
        receipt.cancel("issued in error", AggregateId.newId(), NOW.plusSeconds(60));

        var result = mapper.toResult(receipt);

        assertThat(result.status()).isEqualTo("CANCELLED");
        assertThat(result.cancellationReason()).isEqualTo("issued in error");
    }

    @Test
    void mapsReceiptToSummary() {
        Receipt receipt = newReceipt();

        var summary = mapper.toSummary(receipt);

        assertThat(summary.receiptId()).isEqualTo(receipt.id().value());
        assertThat(summary.number()).isEqualTo("RCT/20260707/00000001");
        assertThat(summary.totalAmountPaise()).isEqualTo(600_000L);
        assertThat(summary.status()).isEqualTo("GENERATED");
    }

    @Test
    void rejectsNullInputs() {
        assertThatThrownBy(() -> mapper.toResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toSummary(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toLineResult(null)).isInstanceOf(NullPointerException.class);
    }

    private Receipt newReceipt() {
        List<ReceiptLine> lines = List.of(
                new ReceiptLine(
                        AggregateId.newId(), ReceiptType.CONTRIBUTION,
                        new ReceiptDescription("Monthly contribution"), Money.inr(500_000)),
                new ReceiptLine(
                        AggregateId.newId(), ReceiptType.PENALTY,
                        new ReceiptDescription("Late payment penalty"), Money.inr(100_000)));
        return Receipt.generate(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new ReceiptNumber("RCT/20260707/00000001"), lines, AggregateId.newId(), NOW);
    }
}
