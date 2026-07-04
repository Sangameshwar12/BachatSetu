package in.bachatsetu.backend.member.domain.event;

import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MemberJoinedGroup(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId groupId,
        GroupRole role,
        Instant occurredAt) implements DomainEvent {

    public MemberJoinedGroup {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
