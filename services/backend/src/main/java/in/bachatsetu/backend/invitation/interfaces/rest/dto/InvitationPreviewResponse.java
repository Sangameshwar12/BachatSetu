package in.bachatsetu.backend.invitation.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record InvitationPreviewResponse(
        @Schema(description = "Group name", example = "Diwali Bachat Gat") String groupName,
        @Schema(description = "Organizer display name", example = "Asha Rao") String organizerName,
        @Schema(description = "Monthly contribution amount, in paise") long contributionAmountPaise,
        @Schema(description = "Currency code", example = "INR") String currencyCode,
        @Schema(description = "Contribution frequency", example = "MONTHLY") String frequency,
        @Schema(description = "Current member count") int memberCount,
        @Schema(description = "Maximum members allowed") int maximumMembers,
        @Schema(description = "Group lifecycle status", example = "ACTIVE") String status) {
}
