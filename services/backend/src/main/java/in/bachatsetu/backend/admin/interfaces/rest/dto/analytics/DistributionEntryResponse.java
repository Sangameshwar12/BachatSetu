package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

/** One category's share of a distribution. */
public record DistributionEntryResponse(
        @Schema(description = "Category key") String key,
        @Schema(description = "Count within this category") long count) {
}
