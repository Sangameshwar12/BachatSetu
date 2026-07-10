package in.bachatsetu.backend.invitation.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Joins a group using exactly one of {@code code} or {@code token}. */
public record AcceptInvitationRequest(
        @Schema(description = "Invitation code, if joining by typed code", example = "AB3D9F2K")
        String code,

        @Schema(description = "Invitation token, if joining by QR or link")
        String token,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.INVITATION_TYPE, message = "must be one of QR, CODE, LINK")
        @Schema(description = "Which channel the caller used to join", example = "CODE")
        String channel) {
}
