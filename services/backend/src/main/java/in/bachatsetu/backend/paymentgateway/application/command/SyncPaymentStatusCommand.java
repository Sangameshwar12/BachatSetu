package in.bachatsetu.backend.paymentgateway.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests an explicit, pull-based re-check of a payment's status with its gateway. */
public record SyncPaymentStatusCommand(AggregateId tenantId, AggregateId paymentId, AggregateId actorId) {

    public SyncPaymentStatusCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
