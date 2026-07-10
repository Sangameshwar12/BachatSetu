package in.bachatsetu.backend.admin.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** Read-only, cross-tenant projection of one savings group, for administrative listing only. */
public record PlatformGroupSummary(
        AggregateId groupId,
        AggregateId tenantId,
        String code,
        String name,
        PlatformGroupStatus status,
        int memberCount,
        Instant createdAt) {

    public PlatformGroupSummary {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (memberCount < 0) {
            throw new IllegalArgumentException("memberCount must not be negative");
        }
    }
}
