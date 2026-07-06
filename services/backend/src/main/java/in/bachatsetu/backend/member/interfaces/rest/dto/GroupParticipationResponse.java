package in.bachatsetu.backend.member.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of one group participation. */
public record GroupParticipationResponse(

        @Schema(description = "Group identifier") String groupId,
        @Schema(description = "Role within the group", example = "MEMBER") String role,
        @Schema(description = "Timestamp the participation started") Instant joinedAt,
        @Schema(description = "Timestamp the participation ended, if applicable") Instant exitedAt,
        @Schema(description = "Participation status", example = "INVITED") String status) {
}
