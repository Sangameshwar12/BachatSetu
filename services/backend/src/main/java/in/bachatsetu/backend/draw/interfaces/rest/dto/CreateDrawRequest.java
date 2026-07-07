package in.bachatsetu.backend.draw.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request to schedule a new draw. */
public record CreateDrawRequest(

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the group the draw belongs to", example = "123e4567-e89b-12d3-a456-426614174000")
        String groupId,

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the monthly cycle the draw belongs to", example = "123e4567-e89b-12d3-a456-426614174000")
        String cycleId,

        @Positive
        @Schema(description = "Draw number; should match the referenced cycle's own cycle number", example = "1")
        int drawNumber,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.DRAW_TYPE)
        @Schema(description = "Draw type", example = "AUCTION")
        String type,

        @NotNull
        @Schema(description = "Timestamp the draw is scheduled to occur", example = "2026-08-01T10:00:00Z")
        Instant scheduledAt) {
}
