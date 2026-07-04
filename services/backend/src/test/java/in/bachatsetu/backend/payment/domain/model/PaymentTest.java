package in.bachatsetu.backend.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.payment.domain.exception.InvalidPaymentStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");

    @Test
    void verifiesAProviderPaymentExactlyOnce() {
        AggregateId actorId = AggregateId.newId();
        Payment payment = newPayment(actorId);
        payment.startAttempt(actorId, NOW.plusSeconds(10));

        payment.verify(
                new ProviderReference("test-provider", "txn-001"),
                actorId,
                NOW.plusSeconds(20));

        assertThat(payment.status()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(payment.reconciliationStatus()).isEqualTo(ReconciliationStatus.MATCHED);
        assertThat(payment.attempts()).singleElement()
                .extracting(PaymentAttempt::status)
                .isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThatThrownBy(() -> payment.verify(
                        new ProviderReference("test-provider", "txn-001"),
                        actorId,
                        NOW.plusSeconds(30)))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void recordsAProviderFailureOnTheCurrentAttempt() {
        AggregateId actorId = AggregateId.newId();
        Payment payment = newPayment(actorId);
        PaymentAttempt attempt = payment.startAttempt(actorId, NOW.plusSeconds(10));

        payment.fail(" provider-declined ", actorId, NOW.plusSeconds(20));

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(attempt.id()).isNotNull();
        assertThat(attempt.sequence()).isEqualTo(1);
        assertThat(attempt.initiatedAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(attempt.providerReference()).isNull();
        assertThat(attempt.failureCode()).isEqualTo("provider-declined");
    }

    @Test
    void rejectsNonPositivePaymentAmounts() {
        AggregateId actorId = AggregateId.newId();

        assertThatThrownBy(() -> Payment.initiate(
                        AggregateId.newId(),
                        AggregateId.newId(),
                        AggregateId.newId(),
                        AggregateId.newId(),
                        new PaymentReference("PAY-12345678"),
                        new IdempotencyKey("checkout-attempt-0001"),
                        Money.zero(Money.INR),
                        PaymentMethod.UPI,
                        actorId,
                        NOW))
                .isInstanceOf(IllegalArgumentException.class);
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
