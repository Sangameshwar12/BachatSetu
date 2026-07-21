package in.bachatsetu.backend.payment.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests that a group's organizer manually record a member's contribution for the current cycle. */
public record RecordManualPaymentCommand(
        AggregateId tenantId,
        AggregateId groupId,
        AggregateId memberId,
        AggregateId actorId) {

    public RecordManualPaymentCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
