package in.bachatsetu.backend.dashboard.application.query;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record CurrentGroupSummary(
        AggregateId groupId,
        String groupCode,
        String name,
        long upcomingInstallmentAmountPaise,
        String currencyCode,
        String frequency,
        int memberCount,
        int maximumMembers) {

    public CurrentGroupSummary {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(groupCode, "groupCode must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        Objects.requireNonNull(frequency, "frequency must not be null");
    }
}
