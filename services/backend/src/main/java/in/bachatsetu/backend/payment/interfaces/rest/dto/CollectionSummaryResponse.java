package in.bachatsetu.backend.payment.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** A group's contribution collection status for its current cycle. */
public record CollectionSummaryResponse(

        @Schema(description = "Group identifier") String groupId,
        @Schema(description = "Whether the group currently has an open contribution cycle") boolean cycleActive,
        @Schema(description = "1-based current cycle number, 0 if no cycle is active") int cycleNumber,
        @Schema(description = "First day of the current cycle") LocalDate cycleStart,
        @Schema(description = "First day of the next cycle (exclusive end)") LocalDate cycleEnd,
        @Schema(description = "Date this cycle's contribution is due") LocalDate dueDate,
        @Schema(description = "Per-member contribution amount in paise") long contributionAmountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Active member count") int totalMembers,
        @Schema(description = "Members who have paid this cycle") int paidCount,
        @Schema(description = "Members not yet due") int pendingCount,
        @Schema(description = "Members past due and unpaid") int overdueCount,
        @Schema(description = "Total expected amount this cycle, in paise") long totalExpectedPaise,
        @Schema(description = "Total amount collected this cycle, in paise") long totalCollectedPaise,
        @Schema(description = "Total amount remaining this cycle, in paise") long totalRemainingPaise,
        @Schema(description = "Per-member contribution status") List<MemberCollectionResponse> members) {

    public CollectionSummaryResponse {
        members = List.copyOf(Objects.requireNonNull(members, "members must not be null"));
    }
}
