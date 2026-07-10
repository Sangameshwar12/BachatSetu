package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

/** One month's count within a trend. */
public record MonthlyMetricResponse(
        @Schema(description = "Calendar month", example = "2026-07") String month,
        @Schema(description = "Count within this month") long count) {
}
