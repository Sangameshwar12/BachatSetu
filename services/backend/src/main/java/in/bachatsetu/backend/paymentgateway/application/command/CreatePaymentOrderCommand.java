package in.bachatsetu.backend.paymentgateway.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;

/**
 * Requests a provider order for an existing payment. {@code confirmedAmount} is the amount the caller
 * believes it is paying; the service validates it against the payment's own recorded amount rather than
 * trusting it outright, so a caller cannot create an order for a different amount than the payment itself.
 */
public record CreatePaymentOrderCommand(
        AggregateId tenantId, AggregateId paymentId, Money confirmedAmount, AggregateId actorId) {

    public CreatePaymentOrderCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(confirmedAmount, "confirmed amount must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
