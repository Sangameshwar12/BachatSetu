package in.bachatsetu.backend.group.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request to add an existing member to a savings group. */
public record AddGroupMemberRequest(

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the member to add", example = "123e4567-e89b-12d3-a456-426614174000")
        String memberId) {
}
