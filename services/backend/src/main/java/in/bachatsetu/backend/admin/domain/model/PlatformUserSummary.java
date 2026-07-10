package in.bachatsetu.backend.admin.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** Read-only, cross-tenant projection of one platform user, for administrative listing only. */
public record PlatformUserSummary(
        AggregateId userId,
        AggregateId tenantId,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        PlatformUserStatus status,
        Instant createdAt) {

    public PlatformUserSummary {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
