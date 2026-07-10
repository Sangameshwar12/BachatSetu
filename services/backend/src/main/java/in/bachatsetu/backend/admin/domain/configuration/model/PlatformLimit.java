package in.bachatsetu.backend.admin.domain.configuration.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** A single configurable platform-wide ceiling, e.g. the maximum number of groups allowed. */
public record PlatformLimit(LimitKey key, long value, long version, Instant updatedAt, AggregateId updatedBy) {

    public PlatformLimit {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
    }

    public static PlatformLimit of(LimitKey key, long value, Instant at) {
        return new PlatformLimit(key, value, 0, at, null);
    }

    public PlatformLimit withValue(long newValue, AggregateId actorId, Instant at) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(at, "at must not be null");
        return new PlatformLimit(key, newValue, version + 1, at, actorId);
    }
}
