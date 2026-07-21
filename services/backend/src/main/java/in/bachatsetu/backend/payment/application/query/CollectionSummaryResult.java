package in.bachatsetu.backend.payment.application.query;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A group's contribution collection status for its current cycle, derived from its own schedule. */
public record CollectionSummaryResult(
        UUID groupId,
        boolean cycleActive,
        int cycleNumber,
        LocalDate cycleStart,
        LocalDate cycleEnd,
        LocalDate dueDate,
        long contributionAmountPaise,
        String currencyCode,
        int totalMembers,
        int paidCount,
        int pendingCount,
        int overdueCount,
        long totalExpectedPaise,
        long totalCollectedPaise,
        long totalRemainingPaise,
        List<MemberCollectionResult> members) {

    public CollectionSummaryResult {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
        members = List.copyOf(Objects.requireNonNull(members, "members must not be null"));
    }
}
