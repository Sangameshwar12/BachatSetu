package in.bachatsetu.backend.group.domain.event;

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Signals creation of an inactive savings group with its owner enrolled. */
public record SavingsGroupCreated(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId tenantId,
        OwnerId ownerId,
        GroupCode groupCode,
        Instant occurredAt) implements DomainEvent {

    public SavingsGroupCreated {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(aggregateId, "aggregate id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(ownerId, "owner id must not be null");
        Objects.requireNonNull(groupCode, "group code must not be null");
        Objects.requireNonNull(occurredAt, "occurred at must not be null");
    }
}
