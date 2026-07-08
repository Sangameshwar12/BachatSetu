package in.bachatsetu.backend.paymentgateway.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests a full refund of a {@code VERIFIED} payment through the gateway it was verified by. */
public record InitiateRefundCommand(AggregateId tenantId, AggregateId paymentId, AggregateId actorId) {

    public InitiateRefundCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
