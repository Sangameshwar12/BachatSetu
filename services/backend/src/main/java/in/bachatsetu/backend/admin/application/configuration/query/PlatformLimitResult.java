package in.bachatsetu.backend.admin.application.configuration.query;

import java.time.Instant;
import java.util.UUID;

/** Application-layer read model mirroring {@code PlatformLimit}. */
public record PlatformLimitResult(String key, long value, long version, Instant updatedAt, UUID updatedBy) {
}
