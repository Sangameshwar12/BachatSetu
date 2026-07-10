package in.bachatsetu.backend.invitation.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record InvitationResponse(
        @Schema(description = "Invitation identifier") String invitationId,
        @Schema(description = "Group identifier") String groupId,
        @Schema(description = "Human-typeable invitation code", example = "AB3D9F2K") String code,
        @Schema(description = "Shareable join link", example = "/join/9f2k...") String joinLink,
        @Schema(description = "How this invitation is shared", example = "CODE") String type,
        @Schema(description = "Invitation lifecycle status", example = "ACTIVE") String status,
        @Schema(description = "When this invitation expires") Instant expiresAt) {
}
