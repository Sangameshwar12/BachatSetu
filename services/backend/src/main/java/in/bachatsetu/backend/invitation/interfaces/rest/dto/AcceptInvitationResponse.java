package in.bachatsetu.backend.invitation.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record AcceptInvitationResponse(
        @Schema(description = "Group identifier the caller just joined") String groupId,
        @Schema(description = "Identifier of the joining member") String memberId,
        @Schema(description = "When the caller joined") Instant joinedAt) {
}
