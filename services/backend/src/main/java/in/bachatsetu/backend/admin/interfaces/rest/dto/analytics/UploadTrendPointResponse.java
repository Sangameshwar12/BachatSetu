package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

/** One day's upload activity within the trailing trend window. */
public record UploadTrendPointResponse(
        @Schema(description = "Calendar date", example = "2026-07-08") String date,
        @Schema(description = "Number of uploads on this date") long count) {
}
