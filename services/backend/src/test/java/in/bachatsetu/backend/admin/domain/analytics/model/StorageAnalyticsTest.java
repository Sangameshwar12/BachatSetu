package in.bachatsetu.backend.admin.domain.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StorageAnalyticsTest {

    @Test
    void recordsEveryField() {
        List<DistributionEntry> providers = List.of(new DistributionEntry("LOCAL", 4));
        List<UploadTrendPoint> uploads = List.of(new UploadTrendPoint(LocalDate.parse("2026-07-08"), 2));

        StorageAnalytics analytics = new StorageAnalytics(4, 4096, 1024.0, providers, uploads);

        assertThat(analytics.totalFiles()).isEqualTo(4);
        assertThat(analytics.totalStorageBytes()).isEqualTo(4096);
        assertThat(analytics.storageProviderDistribution()).hasSize(1);
        assertThat(analytics.uploadsPerDay()).hasSize(1);
    }

    @Test
    void rejectsANullProviderDistribution() {
        assertThatThrownBy(() -> new StorageAnalytics(0, 0, 0.0, null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANegativeTotalBytes() {
        assertThatThrownBy(() -> new StorageAnalytics(0, -1, 0.0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
