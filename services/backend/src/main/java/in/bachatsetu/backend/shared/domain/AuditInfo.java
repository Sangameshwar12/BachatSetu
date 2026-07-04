package in.bachatsetu.backend.shared.domain;

import java.time.Instant;
import java.util.Objects;

public record AuditInfo(
        Instant createdAt,
        AggregateId createdBy,
        Instant updatedAt,
        AggregateId updatedBy) {

    public AuditInfo {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        Objects.requireNonNull(updatedBy, "updatedBy must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
    }

    public static AuditInfo createdBy(AggregateId actorId, Instant createdAt) {
        return new AuditInfo(createdAt, actorId, createdAt, actorId);
    }

    public AuditInfo updatedBy(AggregateId actorId, Instant changedAt) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("changedAt must not precede the last update");
        }
        return new AuditInfo(createdAt, createdBy, changedAt, actorId);
    }
}
