package in.bachatsetu.backend.group.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Requested minimum and maximum active-member bounds for a group rule. */
public record MemberCapacityRequest(

        @Min(2)
        @Schema(description = "Minimum active members required", example = "2")
        int minimum,

        @Min(2)
        @Max(500)
        @Schema(description = "Maximum active members permitted", example = "10")
        int maximum) {
}
