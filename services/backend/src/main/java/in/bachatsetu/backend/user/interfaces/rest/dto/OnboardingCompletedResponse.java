package in.bachatsetu.backend.user.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Confirms the caller's profile onboarding is complete. */
public record OnboardingCompletedResponse(
        @Schema(description = "User identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        String userId,

        @Schema(description = "City", example = "Pune") String city,
        @Schema(description = "State", example = "Maharashtra") String state,
        @Schema(description = "Identifier of the stored profile photo, if any") String photoFileId,
        @Schema(description = "Whether the user wants to receive notifications") boolean notificationsEnabled) {
}
