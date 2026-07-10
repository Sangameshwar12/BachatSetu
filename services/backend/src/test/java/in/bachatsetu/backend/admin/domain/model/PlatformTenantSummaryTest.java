package in.bachatsetu.backend.admin.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import org.junit.jupiter.api.Test;

class PlatformTenantSummaryTest {

    @Test
    void recordsTenantTotals() {
        AggregateId tenantId = AggregateId.newId();

        PlatformTenantSummary summary = new PlatformTenantSummary(tenantId, 5, 2);

        assertThat(summary.tenantId()).isEqualTo(tenantId);
        assertThat(summary.userCount()).isEqualTo(5);
        assertThat(summary.groupCount()).isEqualTo(2);
    }

    @Test
    void rejectsANullTenantId() {
        assertThatThrownBy(() -> new PlatformTenantSummary(null, 0, 0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANegativeUserCount() {
        assertThatThrownBy(() -> new PlatformTenantSummary(AggregateId.newId(), -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeGroupCount() {
        assertThatThrownBy(() -> new PlatformTenantSummary(AggregateId.newId(), 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
