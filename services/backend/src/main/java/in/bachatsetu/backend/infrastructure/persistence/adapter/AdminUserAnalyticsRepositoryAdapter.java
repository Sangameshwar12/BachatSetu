package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import in.bachatsetu.backend.admin.domain.analytics.model.MonthlyMetric;
import in.bachatsetu.backend.admin.domain.analytics.model.TenantUserCount;
import in.bachatsetu.backend.admin.domain.analytics.model.UserAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.port.UserAnalyticsRepository;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes platform user analytics from {@link UserSpringDataRepository} — no SQL view, no caching, no
 * scheduled aggregation.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminUserAnalyticsRepositoryAdapter implements UserAnalyticsRepository {

    private final UserSpringDataRepository repository;

    public AdminUserAnalyticsRepositoryAdapter(UserSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public UserAnalytics compute() {
        return new UserAnalytics(
                repository.countByDeletedFalse(),
                repository.countByAuthenticationStatusAndDeletedFalse(UserStatus.ACTIVE),
                repository.countByAuthenticationStatusAndDeletedFalse(UserStatus.DISABLED),
                findMonthlyRegistrations(),
                findLanguageDistribution(),
                findUsersPerTenant());
    }

    private List<MonthlyMetric> findMonthlyRegistrations() {
        return repository.findMonthlyRegistrationCounts().stream()
                .map(row -> new MonthlyMetric(
                        YearMonth.of(toInt(row[0]), toInt(row[1])), ((Number) row[2]).longValue()))
                .toList();
    }

    private List<DistributionEntry> findLanguageDistribution() {
        return repository.findPreferredLanguageDistribution().stream()
                .map(row -> new DistributionEntry(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    private List<TenantUserCount> findUsersPerTenant() {
        return repository.findUserCountsByTenant().stream()
                .map(row -> new TenantUserCount(new AggregateId((UUID) row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    private static int toInt(Object value) {
        return ((Number) value).intValue();
    }
}
