package in.bachatsetu.backend.receipt.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.receipt.domain.exception.InvalidReceiptStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceiptTest {

    private static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    @Test
    void generatesAReceiptWithATotalDerivedFromItsLines() {
        Receipt receipt = newReceipt();

        assertThat(receipt.status()).isEqualTo(ReceiptStatus.GENERATED);
        assertThat(receipt.total()).isEqualTo(Money.inr(600_000));
        assertThat(receipt.lines()).hasSize(2);
        assertThat(receipt.pullDomainEvents()).hasSize(1);
    }

    @Test
    void rejectsAnEmptyLineList() {
        assertThatThrownBy(() -> Receipt.generate(
                        AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                        new ReceiptNumber("RCT/20260707/00000001"), List.of(), AggregateId.newId(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marksAGeneratedReceiptAsDelivered() {
        Receipt receipt = newReceipt();

        receipt.markDelivered(AggregateId.newId(), NOW.plusSeconds(60));

        assertThat(receipt.status()).isEqualTo(ReceiptStatus.DELIVERED);
    }

    @Test
    void rejectsDeliveringATwiceDeliveredReceipt() {
        Receipt receipt = newReceipt();
        receipt.markDelivered(AggregateId.newId(), NOW.plusSeconds(60));

        assertThatThrownBy(() -> receipt.markDelivered(AggregateId.newId(), NOW.plusSeconds(120)))
                .isInstanceOf(InvalidReceiptStateException.class);
    }

    @Test
    void cancelsAGeneratedReceiptWithAReason() {
        Receipt receipt = newReceipt();
        receipt.pullDomainEvents();

        receipt.cancel("issued in error", AggregateId.newId(), NOW.plusSeconds(60));

        assertThat(receipt.status()).isEqualTo(ReceiptStatus.CANCELLED);
        assertThat(receipt.cancellationReason()).isEqualTo("issued in error");
        assertThat(receipt.pullDomainEvents()).hasSize(1);
    }

    @Test
    void rejectsCancellingAnAlreadyCancelledReceipt() {
        Receipt receipt = newReceipt();
        receipt.cancel("issued in error", AggregateId.newId(), NOW.plusSeconds(60));

        assertThatThrownBy(() -> receipt.cancel("again", AggregateId.newId(), NOW.plusSeconds(120)))
                .isInstanceOf(InvalidReceiptStateException.class);
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
