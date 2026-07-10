package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.analytics.model.PaymentAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.PaymentTrendPoint;
import in.bachatsetu.backend.admin.domain.analytics.port.PaymentAnalyticsRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes payment analytics from {@link PaymentSpringDataRepository} — no SQL view, no caching, no
 * scheduled aggregation. Every rate is a fraction in {@code [0, 1]}, {@code 0} when there are no payments at
 * all.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminPaymentAnalyticsRepositoryAdapter implements PaymentAnalyticsRepository {

    private static final int TREND_WINDOW_DAYS = 30;

    private final PaymentSpringDataRepository repository;

    public AdminPaymentAnalyticsRepositoryAdapter(PaymentSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public PaymentAnalytics compute() {
        long totalCount = repository.countByDeletedFalse();
        long verifiedCount = repository.countByStatusAndDeletedFalse(PaymentStatus.VERIFIED);
        long failedCount = repository.countByStatusAndDeletedFalse(PaymentStatus.FAILED);
        long pendingCount = repository.countByStatusAndDeletedFalse(PaymentStatus.INITIATED)
                + repository.countByStatusAndDeletedFalse(PaymentStatus.PENDING_PROVIDER);
        long verifiedVolume = repository.sumAmountPaiseByStatus(PaymentStatus.VERIFIED);

        return new PaymentAnalytics(
                repository.sumAmountPaise(),
                verifiedVolume,
                failedCount,
                pendingCount,
                verifiedCount == 0 ? 0.0 : (double) verifiedVolume / verifiedCount,
                totalCount == 0 ? 0.0 : (double) verifiedCount / totalCount,
                totalCount == 0 ? 0.0 : (double) failedCount / totalCount,
                findTrend());
    }

    private List<PaymentTrendPoint> findTrend() {
        Instant since = Instant.now().minus(TREND_WINDOW_DAYS, ChronoUnit.DAYS);
        return repository.findDailyPaymentTrend(since).stream()
                .map(row -> new PaymentTrendPoint(
                        LocalDate.of(toInt(row[0]), toInt(row[1]), toInt(row[2])),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()))
                .toList();
    }

    private static int toInt(Object value) {
        return ((Number) value).intValue();
    }
}
