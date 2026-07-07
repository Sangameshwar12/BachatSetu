package in.bachatsetu.backend.payment.application.command;

import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;

/** Requests initiation of a new Payment aggregate. */
public record CreatePaymentCommand(
        AggregateId tenantId,
        AggregateId groupId,
        AggregateId memberId,
        IdempotencyKey idempotencyKey,
        Money amount,
        PaymentMethod method,
        AggregateId actorId) {

    public CreatePaymentCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotency key must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
