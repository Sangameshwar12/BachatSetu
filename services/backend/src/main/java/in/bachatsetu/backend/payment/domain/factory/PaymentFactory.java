package in.bachatsetu.backend.payment.domain.factory;

import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class PaymentFactory {

    private final Clock clock;

    public PaymentFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Payment initiate(
            AggregateId tenantId,
            AggregateId groupId,
            AggregateId memberId,
            IdempotencyKey idempotencyKey,
            Money amount,
            PaymentMethod method,
            AggregateId actorId) {
        PaymentReference reference = new PaymentReference(
                "PAY-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        return Payment.initiate(
                AggregateId.newId(),
                tenantId,
                groupId,
                memberId,
                reference,
                idempotencyKey,
                amount,
                method,
                actorId,
                clock.instant());
    }
}
