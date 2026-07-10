package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.analytics.model.UserAnalytics;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminUserAnalyticsRepositoryAdapterTest {

    @Test
    void computesUserAnalyticsIncludingDistributionsAndTenantCounts() {
        UserSpringDataRepository repository = mock(UserSpringDataRepository.class);
        UUID tenantId = UUID.randomUUID();
        when(repository.countByDeletedFalse()).thenReturn(10L);
        when(repository.countByAuthenticationStatusAndDeletedFalse(UserStatus.ACTIVE)).thenReturn(8L);
        when(repository.countByAuthenticationStatusAndDeletedFalse(UserStatus.DISABLED)).thenReturn(2L);
        when(repository.findMonthlyRegistrationCounts())
                .thenReturn(List.<Object[]>of(new Object[] {2026, 7, 5L}));
        when(repository.findPreferredLanguageDistribution())
                .thenReturn(List.<Object[]>of(new Object[] {PreferredLanguage.ENGLISH, 7L}));
        when(repository.findUserCountsByTenant())
                .thenReturn(List.<Object[]>of(new Object[] {tenantId, 10L}));
        AdminUserAnalyticsRepositoryAdapter adapter = new AdminUserAnalyticsRepositoryAdapter(repository);

        UserAnalytics analytics = adapter.compute();

        assertThat(analytics.totalUsers()).isEqualTo(10L);
        assertThat(analytics.activeUsers()).isEqualTo(8L);
        assertThat(analytics.disabledUsers()).isEqualTo(2L);
        assertThat(analytics.monthlyRegistrations()).hasSize(1);
        assertThat(analytics.preferredLanguageDistribution()).hasSize(1);
        assertThat(analytics.preferredLanguageDistribution().get(0).key()).isEqualTo("ENGLISH");
        assertThat(analytics.usersPerTenant()).hasSize(1);
        assertThat(analytics.usersPerTenant().get(0).tenantId().value()).isEqualTo(tenantId);
    }
}
