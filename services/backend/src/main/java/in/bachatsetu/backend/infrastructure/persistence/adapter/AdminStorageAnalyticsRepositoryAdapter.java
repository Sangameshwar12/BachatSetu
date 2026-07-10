package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import in.bachatsetu.backend.admin.domain.analytics.model.StorageAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.UploadTrendPoint;
import in.bachatsetu.backend.admin.domain.analytics.port.StorageAnalyticsRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes storage analytics from {@link StoredFileSpringDataRepository} — no SQL view, no caching, no
 * scheduled aggregation.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminStorageAnalyticsRepositoryAdapter implements StorageAnalyticsRepository {

    private static final int TREND_WINDOW_DAYS = 30;

    private final StoredFileSpringDataRepository repository;

    public AdminStorageAnalyticsRepositoryAdapter(StoredFileSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public StorageAnalytics compute() {
        long totalFiles = repository.countByDeletedFalse();
        long totalBytes = repository.sumSize();

        return new StorageAnalytics(
                totalFiles,
                totalBytes,
                totalFiles == 0 ? 0.0 : (double) totalBytes / totalFiles,
                findProviderDistribution(),
                findUploadTrend());
    }

    private List<DistributionEntry> findProviderDistribution() {
        return repository.findProviderDistribution().stream()
                .map(row -> new DistributionEntry(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    private List<UploadTrendPoint> findUploadTrend() {
        Instant since = Instant.now().minus(TREND_WINDOW_DAYS, ChronoUnit.DAYS);
        return repository.findDailyUploadTrend(since).stream()
                .map(row -> new UploadTrendPoint(
                        LocalDate.of(toInt(row[0]), toInt(row[1]), toInt(row[2])), ((Number) row[3]).longValue()))
                .toList();
    }

    private static int toInt(Object value) {
        return ((Number) value).intValue();
    }
}
