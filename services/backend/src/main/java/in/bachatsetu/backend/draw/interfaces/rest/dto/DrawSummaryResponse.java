package in.bachatsetu.backend.draw.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Compact presentation view of a draw returned by list endpoints. */
public record DrawSummaryResponse(

        @Schema(description = "Draw identifier") String drawId,
        @Schema(description = "Draw number") int drawNumber,
        @Schema(description = "Draw type", example = "AUCTION") String type,
        @Schema(description = "Lifecycle status", example = "SCHEDULED") String status,
        @Schema(description = "Timestamp the draw is scheduled to occur") Instant scheduledAt,
        @Schema(description = "Identifier of the winning member, once completed") String winnerMemberId) {
}
