package in.bachatsetu.backend.group.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request to create a new savings group owned by the authenticated caller. */
public record CreateSavingsGroupRequest(

        @NotBlank
        @Size(min = 3, max = 100)
        @Schema(description = "Group name", example = "Sunrise Bhishi Circle")
        String name,

        @Size(max = 500)
        @Schema(description = "Optional group description", example = "Monthly savings for our society")
        String description,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.GROUP_TYPE, message = "must be a supported group type")
        @Schema(description = "Group type", example = "BHISHI")
        String type,

        @NotNull
        @Valid
        GroupRuleRequest rule) {
}
