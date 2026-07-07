package in.bachatsetu.backend.member.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request to update the editable lifecycle status of a member profile. */
public record UpdateMemberProfileRequest(

        @NotBlank
        @Pattern(regexp = ValidationPatterns.MEMBER_STATUS)
        @Schema(description = "New lifecycle status", example = "ACTIVE")
        String status) {
}
