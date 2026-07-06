package in.bachatsetu.backend.group.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of one group membership. */
public record GroupMemberResponse(

        @Schema(description = "Member identifier") String memberId,
        @Schema(description = "Timestamp the member joined") Instant joinedAt,
        @Schema(description = "Timestamp the member was removed, if applicable") Instant removedAt,
        @Schema(description = "Whether the membership is currently active") boolean active) {
}
