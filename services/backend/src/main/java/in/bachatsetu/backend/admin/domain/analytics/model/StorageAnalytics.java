package in.bachatsetu.backend.admin.domain.analytics.model;

import java.util.List;
import java.util.Objects;

/** Storage analytics. */
public record StorageAnalytics(
        long totalFiles,
        long totalStorageBytes,
        double averageFileSizeBytes,
        List<DistributionEntry> storageProviderDistribution,
        List<UploadTrendPoint> uploadsPerDay) {

    public StorageAnalytics {
        if (totalFiles < 0) {
            throw new IllegalArgumentException("totalFiles must not be negative");
        }
        if (totalStorageBytes < 0) {
            throw new IllegalArgumentException("totalStorageBytes must not be negative");
        }
        storageProviderDistribution = List.copyOf(Objects.requireNonNull(
                storageProviderDistribution, "storageProviderDistribution must not be null"));
        uploadsPerDay = List.copyOf(Objects.requireNonNull(uploadsPerDay, "uploadsPerDay must not be null"));
    }
}
