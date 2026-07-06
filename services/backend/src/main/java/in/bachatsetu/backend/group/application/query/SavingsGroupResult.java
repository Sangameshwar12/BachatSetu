package in.bachatsetu.backend.group.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Complete application read model for a savings group. */
public record SavingsGroupResult(
        UUID groupId,
        UUID tenantId,
        UUID ownerId,
        String groupCode,
        String name,
        String description,
        String type,
        String status,
        long contributionAmountPaise,
        String currencyCode,
        int maximumMembers,
        int activeMemberCount,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<GroupMemberResult> members) {

    public SavingsGroupResult {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(ownerId, "owner id must not be null");
        Objects.requireNonNull(groupCode, "group code must not be null");
        Objects.requireNonNull(name, "group name must not be null");
        Objects.requireNonNull(description, "group description must not be null");
        Objects.requireNonNull(type, "group type must not be null");
        Objects.requireNonNull(status, "group status must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
        Objects.requireNonNull(createdAt, "created at must not be null");
        Objects.requireNonNull(updatedAt, "updated at must not be null");
        members = List.copyOf(Objects.requireNonNull(members, "members must not be null"));
    }
}
