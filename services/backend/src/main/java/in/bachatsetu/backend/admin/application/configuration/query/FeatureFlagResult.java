package in.bachatsetu.backend.admin.application.configuration.query;

import java.time.Instant;
import java.util.UUID;

/** Application-layer read model mirroring {@code FeatureFlag}. */
public record FeatureFlagResult(String key, boolean enabled, long version, Instant updatedAt, UUID updatedBy) {
}
