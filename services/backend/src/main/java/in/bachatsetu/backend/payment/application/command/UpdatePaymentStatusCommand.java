package in.bachatsetu.backend.payment.application.command;

import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests a lifecycle status change for an existing payment. */
public record UpdatePaymentStatusCommand(
        AggregateId tenantId,
        AggregateId paymentId,
        PaymentStatus targetStatus,
        ProviderReference providerReference,
        String failureCode,
        AggregateId actorId) {

    public UpdatePaymentStatusCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(targetStatus, "target status must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
