package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Storage analytics. */
public record StorageAnalyticsResponse(

        @Schema(description = "Total number of stored files") long totalFiles,
        @Schema(description = "Total storage used, in bytes") long totalStorageBytes,
        @Schema(description = "Average file size, in bytes") double averageFileSizeBytes,
        @Schema(description = "Storage provider distribution")
        List<DistributionEntryResponse> storageProviderDistribution,
        @Schema(description = "Daily upload activity for the trailing 30 days")
        List<UploadTrendPointResponse> uploadsPerDay) {

    public StorageAnalyticsResponse {
        storageProviderDistribution = List.copyOf(Objects.requireNonNull(
                storageProviderDistribution, "storageProviderDistribution must not be null"));
        uploadsPerDay = List.copyOf(Objects.requireNonNull(uploadsPerDay, "uploadsPerDay must not be null"));
    }
}
