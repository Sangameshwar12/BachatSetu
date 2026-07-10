package in.bachatsetu.backend.user.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request to complete the post-signup profile onboarding step. */
public record CompleteOnboardingRequest(
        @Size(max = 100)
        @Schema(description = "City", example = "Pune")
        String city,

        @Size(max = 100)
        @Schema(description = "State", example = "Maharashtra")
        String state,

        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of a photo already uploaded via the storage module",
                example = "123e4567-e89b-12d3-a456-426614174000")
        String photoFileId,

        @Schema(description = "Whether the user wants to receive notifications", example = "true")
        boolean notificationsEnabled) {
}
