package in.bachatsetu.backend.member.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request to create a member profile together with its first group participation. */
public record CreateMemberProfileRequest(

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the user becoming a member", example = "123e4567-e89b-12d3-a456-426614174000")
        String userId,

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the group to join", example = "123e4567-e89b-12d3-a456-426614174000")
        String groupId,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.GROUP_ROLE)
        @Schema(description = "Role within the group", example = "MEMBER")
        String role) {
}
