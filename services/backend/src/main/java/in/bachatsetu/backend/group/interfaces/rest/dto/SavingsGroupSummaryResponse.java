package in.bachatsetu.backend.group.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Compact savings group view returned by the list endpoint. */
public record SavingsGroupSummaryResponse(

        @Schema(description = "Group identifier") String groupId,
        @Schema(description = "Human-facing group code", example = "BS-1A2B3C4D5E6F7A8B") String groupCode,
        @Schema(description = "Group name") String name,
        @Schema(description = "Lifecycle status", example = "ACTIVE") String status,
        @Schema(description = "Contribution amount in paise") long contributionAmountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Maximum active members") int maximumMembers,
        @Schema(description = "Current active member count") int activeMemberCount) {
}
