package in.bachatsetu.backend.payment.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private final PaymentApplicationMapper mapper = new PaymentApplicationMapper();

    @Test
    void mapsPaymentToResultIncludingAttempts() {
        AggregateId actorId = AggregateId.newId();
        Payment payment = newPayment(actorId);
        payment.startAttempt(actorId, NOW.plusSeconds(1));
        payment.verify(new ProviderReference("test-provider", "txn-001"), actorId, NOW.plusSeconds(2));

        var result = mapper.toResult(payment);

        assertThat(result.paymentId()).isEqualTo(payment.id().value());
        assertThat(result.reference()).isEqualTo(payment.reference().value());
        assertThat(result.amountPaise()).isEqualTo(100_000L);
        assertThat(result.currencyCode()).isEqualTo("INR");
        assertThat(result.status()).isEqualTo("VERIFIED");
        assertThat(result.reconciliationStatus()).isEqualTo("MATCHED");
        assertThat(result.attempts()).singleElement().satisfies(attempt -> {
            assertThat(attempt.status()).isEqualTo("SUCCEEDED");
            assertThat(attempt.provider()).isEqualTo("test-provider");
            assertThat(attempt.transactionId()).isEqualTo("txn-001");
        });
        assertThatThrownBy(() -> result.attempts().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapsPaymentToSummary() {
        Payment payment = newPayment(AggregateId.newId());

        var summary = mapper.toSummary(payment);

        assertThat(summary.paymentId()).isEqualTo(payment.id().value());
        assertThat(summary.reference()).isEqualTo(payment.reference().value());
        assertThat(summary.status()).isEqualTo("INITIATED");
    }

    @Test
    void rejectsNullInputs() {
        assertThatThrownBy(() -> mapper.toResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toSummary(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toAttemptResult(null)).isInstanceOf(NullPointerException.class);
    }

    private Payment newPayment(AggregateId actorId) {
        return Payment.initiate(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new PaymentReference("PAY-12345678"),
                new IdempotencyKey("checkout-attempt-0001"),
                Money.inr(100_000),
                PaymentMethod.UPI,
                actorId,
                NOW);
    }
}
