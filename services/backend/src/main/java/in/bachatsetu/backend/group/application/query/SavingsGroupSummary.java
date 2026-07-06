package in.bachatsetu.backend.group.application.query;

import java.util.Objects;
import java.util.UUID;

/** Compact savings group view optimized for list use cases. */
public record SavingsGroupSummary(
        UUID groupId,
        String groupCode,
        String name,
        String status,
        long contributionAmountPaise,
        String currencyCode,
        int maximumMembers,
        int activeMemberCount) {

    public SavingsGroupSummary {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(groupCode, "group code must not be null");
        Objects.requireNonNull(name, "group name must not be null");
        Objects.requireNonNull(status, "group status must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
    }
}
