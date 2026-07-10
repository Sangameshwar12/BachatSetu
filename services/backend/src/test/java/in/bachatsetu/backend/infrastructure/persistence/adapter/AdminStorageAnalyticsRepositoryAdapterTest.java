package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.analytics.model.StorageAnalytics;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminStorageAnalyticsRepositoryAdapterTest {

    @Test
    void computesStorageAnalyticsIncludingAverageSizeAndTrend() {
        StoredFileSpringDataRepository repository = mock(StoredFileSpringDataRepository.class);
        when(repository.countByDeletedFalse()).thenReturn(4L);
        when(repository.sumSize()).thenReturn(4096L);
        when(repository.findProviderDistribution())
                .thenReturn(List.<Object[]>of(new Object[] {StorageProvider.LOCAL, 4L}));
        when(repository.findDailyUploadTrend(any()))
                .thenReturn(List.<Object[]>of(new Object[] {2026, 7, 8, 2L}));
        AdminStorageAnalyticsRepositoryAdapter adapter = new AdminStorageAnalyticsRepositoryAdapter(repository);

        StorageAnalytics analytics = adapter.compute();

        assertThat(analytics.totalFiles()).isEqualTo(4L);
        assertThat(analytics.totalStorageBytes()).isEqualTo(4096L);
        assertThat(analytics.averageFileSizeBytes()).isEqualTo(1024.0);
        assertThat(analytics.storageProviderDistribution()).hasSize(1);
        assertThat(analytics.uploadsPerDay()).hasSize(1);
        assertThat(analytics.uploadsPerDay().get(0).count()).isEqualTo(2L);
    }

    @Test
    void returnsZeroAverageWhenThereAreNoFiles() {
        StoredFileSpringDataRepository repository = mock(StoredFileSpringDataRepository.class);
        when(repository.countByDeletedFalse()).thenReturn(0L);
        when(repository.sumSize()).thenReturn(0L);
        when(repository.findProviderDistribution()).thenReturn(List.of());
        when(repository.findDailyUploadTrend(any())).thenReturn(List.of());
        AdminStorageAnalyticsRepositoryAdapter adapter = new AdminStorageAnalyticsRepositoryAdapter(repository);

        assertThat(adapter.compute().averageFileSizeBytes()).isZero();
    }
}
