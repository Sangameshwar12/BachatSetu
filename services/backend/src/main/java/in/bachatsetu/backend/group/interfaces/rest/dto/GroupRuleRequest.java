package in.bachatsetu.backend.group.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Requested operating rule for a new savings group. */
public record GroupRuleRequest(

        @NotNull
        @Valid
        ContributionScheduleRequest contributionSchedule,

        @NotNull
        @Valid
        MemberCapacityRequest memberCapacity,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.PAYOUT_METHOD, message = "must be a supported payout method")
        @Schema(description = "Payout method", example = "RANDOM_DRAW")
        String payoutMethod,

        @Schema(description = "Whether partial contributions are accepted", example = "false")
        boolean partialPaymentsAllowed) {
}
