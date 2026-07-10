package in.bachatsetu.backend.admin.application.query;

import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Application-layer read model for one savings group. */
public record PlatformGroupResult(
        UUID groupId,
        UUID tenantId,
        String code,
        String name,
        PlatformGroupStatus status,
        int memberCount,
        Instant createdAt) {

    public PlatformGroupResult {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
