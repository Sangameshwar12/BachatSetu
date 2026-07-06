package in.bachatsetu.backend.group.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** Requested recurring contribution configuration for a new group. */
public record ContributionScheduleRequest(

        @NotNull
        @Positive
        @Schema(description = "Contribution amount in paise", example = "500000")
        Long contributionAmountPaise,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.CONTRIBUTION_FREQUENCY, message = "must be a supported contribution frequency")
        @Schema(description = "Contribution frequency", example = "MONTHLY")
        String frequency,

        @NotNull
        @Schema(description = "Date the first collection cycle begins", example = "2026-08-01")
        LocalDate startDate,

        @Min(1)
        @Max(120)
        @Schema(description = "Number of collection cycles in the group", example = "12")
        int cycleCount) {
}
