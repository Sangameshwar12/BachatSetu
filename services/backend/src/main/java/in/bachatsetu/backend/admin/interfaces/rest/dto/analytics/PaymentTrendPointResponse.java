package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

/** One day's payment activity within the trailing trend window. */
public record PaymentTrendPointResponse(
        @Schema(description = "Calendar date", example = "2026-07-08") String date,
        @Schema(description = "Number of payments on this date") long count,
        @Schema(description = "Total payment volume, in paise, on this date") long volumePaise) {
}
