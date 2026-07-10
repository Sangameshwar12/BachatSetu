package in.bachatsetu.backend.admin.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformGroupSummaryTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void recordsEveryField() {
        AggregateId groupId = AggregateId.newId();
        AggregateId tenantId = AggregateId.newId();

        PlatformGroupSummary summary = new PlatformGroupSummary(
                groupId, tenantId, "GRP-1", "Neighborhood Bhishi", PlatformGroupStatus.ACTIVE, 12, NOW);

        assertThat(summary.groupId()).isEqualTo(groupId);
        assertThat(summary.code()).isEqualTo("GRP-1");
        assertThat(summary.memberCount()).isEqualTo(12);
    }

    @Test
    void rejectsANullGroupId() {
        assertThatThrownBy(() -> new PlatformGroupSummary(
                        null, AggregateId.newId(), "GRP-1", "name", PlatformGroupStatus.ACTIVE, 0, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANegativeMemberCount() {
        assertThatThrownBy(() -> new PlatformGroupSummary(
                        AggregateId.newId(), AggregateId.newId(), "GRP-1", "name", PlatformGroupStatus.ACTIVE, -1,
                        NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
