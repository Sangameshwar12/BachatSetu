package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.analytics.model.PaymentAnalytics;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminPaymentAnalyticsRepositoryAdapterTest {

    @Test
    void computesPaymentAnalyticsIncludingRatesAndTrend() {
        PaymentSpringDataRepository repository = mock(PaymentSpringDataRepository.class);
        when(repository.countByDeletedFalse()).thenReturn(20L);
        when(repository.countByStatusAndDeletedFalse(PaymentStatus.VERIFIED)).thenReturn(15L);
        when(repository.countByStatusAndDeletedFalse(PaymentStatus.FAILED)).thenReturn(2L);
        when(repository.countByStatusAndDeletedFalse(PaymentStatus.INITIATED)).thenReturn(2L);
        when(repository.countByStatusAndDeletedFalse(PaymentStatus.PENDING_PROVIDER)).thenReturn(1L);
        when(repository.sumAmountPaise()).thenReturn(200_000L);
        when(repository.sumAmountPaiseByStatus(PaymentStatus.VERIFIED)).thenReturn(150_000L);
        when(repository.findDailyPaymentTrend(any()))
                .thenReturn(List.<Object[]>of(new Object[] {2026, 7, 8, 3L, 15_000L}));
        AdminPaymentAnalyticsRepositoryAdapter adapter = new AdminPaymentAnalyticsRepositoryAdapter(repository);

        PaymentAnalytics analytics = adapter.compute();

        assertThat(analytics.totalPaymentVolumePaise()).isEqualTo(200_000L);
        assertThat(analytics.verifiedPaymentVolumePaise()).isEqualTo(150_000L);
        assertThat(analytics.failedPaymentCount()).isEqualTo(2L);
        assertThat(analytics.pendingPaymentCount()).isEqualTo(3L);
        assertThat(analytics.averageContributionPaise()).isEqualTo(10_000.0);
        assertThat(analytics.paymentSuccessRate()).isEqualTo(0.75);
        assertThat(analytics.paymentFailureRate()).isEqualTo(0.1);
        assertThat(analytics.paymentTrend()).hasSize(1);
        assertThat(analytics.paymentTrend().get(0).count()).isEqualTo(3L);
        assertThat(analytics.paymentTrend().get(0).volumePaise()).isEqualTo(15_000L);
    }

    @Test
    void returnsZeroRatesWhenThereAreNoPayments() {
        PaymentSpringDataRepository repository = mock(PaymentSpringDataRepository.class);
        when(repository.countByDeletedFalse()).thenReturn(0L);
        when(repository.sumAmountPaise()).thenReturn(0L);
        when(repository.findDailyPaymentTrend(any())).thenReturn(List.of());
        AdminPaymentAnalyticsRepositoryAdapter adapter = new AdminPaymentAnalyticsRepositoryAdapter(repository);

        PaymentAnalytics analytics = adapter.compute();

        assertThat(analytics.paymentSuccessRate()).isZero();
        assertThat(analytics.paymentFailureRate()).isZero();
        assertThat(analytics.averageContributionPaise()).isZero();
    }
}
