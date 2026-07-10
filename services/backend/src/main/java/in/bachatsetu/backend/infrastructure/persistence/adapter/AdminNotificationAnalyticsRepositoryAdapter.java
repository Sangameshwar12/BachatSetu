package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import in.bachatsetu.backend.admin.domain.analytics.model.NotificationAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.port.NotificationAnalyticsRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.notification.domain.model.NotificationStatus;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes notification analytics from {@link NotificationSpringDataRepository} — no SQL view, no caching,
 * no scheduled aggregation.
 *
 * <p>{@code unreadNotifications} approximates "unread" as "not yet delivered": the Notification module
 * tracks only delivery lifecycle, not a recipient read receipt. See {@code docs/application/admin-analytics.md}.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminNotificationAnalyticsRepositoryAdapter implements NotificationAnalyticsRepository {

    private final NotificationSpringDataRepository repository;

    public AdminNotificationAnalyticsRepositoryAdapter(NotificationSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public NotificationAnalytics compute() {
        long total = repository.countByDeletedFalse();
        List<DistributionEntry> statusCounts = findStatusDistribution();
        long delivered = statusCounts.stream()
                .filter(entry -> entry.key().equals(NotificationStatus.DELIVERED.name()))
                .mapToLong(DistributionEntry::count)
                .sum();

        return new NotificationAnalytics(total, total - delivered, statusCounts, findCategoryDistribution());
    }

    private List<DistributionEntry> findStatusDistribution() {
        return repository.findStatusDistribution().stream()
                .map(row -> new DistributionEntry(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    private List<DistributionEntry> findCategoryDistribution() {
        return repository.findCategoryDistribution().stream()
                .map(row -> new DistributionEntry(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }
}
