package in.bachatsetu.backend.admin.application.query;

import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Application-layer read model for one platform user. */
public record PlatformUserResult(
        UUID userId,
        UUID tenantId,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        PlatformUserStatus status,
        Instant createdAt) {

    public PlatformUserResult {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
