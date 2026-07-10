package in.bachatsetu.backend.admin.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlatformStatisticsTest {

    @Test
    void recordsEveryTotal() {
        PlatformStatistics statistics = new PlatformStatistics(10, 8, 2, 5, 4, 20, 15, 15, 30, 7);

        assertThat(statistics.totalUsers()).isEqualTo(10);
        assertThat(statistics.activeUsers()).isEqualTo(8);
        assertThat(statistics.disabledUsers()).isEqualTo(2);
        assertThat(statistics.completedPayments()).isEqualTo(15);
        assertThat(statistics.totalFiles()).isEqualTo(7);
    }

    @Test
    void rejectsANegativeTotal() {
        assertThatThrownBy(() -> new PlatformStatistics(-1, 0, 0, 0, 0, 0, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
