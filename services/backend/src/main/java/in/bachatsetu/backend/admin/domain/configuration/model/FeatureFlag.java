package in.bachatsetu.backend.admin.domain.configuration.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** Whether a single platform feature is currently enabled, and who last changed it. */
public record FeatureFlag(FeatureKey key, boolean enabled, long version, Instant updatedAt, AggregateId updatedBy) {

    public FeatureFlag {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static FeatureFlag defaultEnabled(FeatureKey key, Instant at) {
        return new FeatureFlag(key, true, 0, at, null);
    }

    public FeatureFlag withEnabled(boolean newEnabled, AggregateId actorId, Instant at) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(at, "at must not be null");
        return new FeatureFlag(key, newEnabled, version + 1, at, actorId);
    }
}
