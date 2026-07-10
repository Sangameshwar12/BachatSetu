package in.bachatsetu.backend.invitation.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateInvitationRequest(
        @NotBlank
        @Pattern(regexp = ValidationPatterns.INVITATION_TYPE, message = "must be one of QR, CODE, LINK")
        @Schema(description = "How this invitation will primarily be shared", example = "CODE")
        String type) {
}
