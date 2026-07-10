package in.bachatsetu.backend.admin.application.analytics.query;

import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import in.bachatsetu.backend.admin.domain.analytics.model.UploadTrendPoint;
import java.util.List;
import java.util.Objects;

/** Application-layer read model mirroring {@code StorageAnalytics}. */
public record StorageAnalyticsResult(
        long totalFiles,
        long totalStorageBytes,
        double averageFileSizeBytes,
        List<DistributionEntry> storageProviderDistribution,
        List<UploadTrendPoint> uploadsPerDay) {

    public StorageAnalyticsResult {
        storageProviderDistribution = List.copyOf(Objects.requireNonNull(
                storageProviderDistribution, "storageProviderDistribution must not be null"));
        uploadsPerDay = List.copyOf(Objects.requireNonNull(uploadsPerDay, "uploadsPerDay must not be null"));
    }
}
