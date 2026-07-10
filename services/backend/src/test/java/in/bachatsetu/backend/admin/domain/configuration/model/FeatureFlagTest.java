package in.bachatsetu.backend.admin.domain.configuration.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FeatureFlagTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void defaultEnabledStartsTrueWithNoActor() {
        FeatureFlag flag = FeatureFlag.defaultEnabled(FeatureKey.PAYMENTS, NOW);

        assertThat(flag.key()).isEqualTo(FeatureKey.PAYMENTS);
        assertThat(flag.enabled()).isTrue();
        assertThat(flag.version()).isZero();
        assertThat(flag.updatedBy()).isNull();
    }

    @Test
    void withEnabledIncrementsVersionAndRecordsActor() {
        FeatureFlag flag = FeatureFlag.defaultEnabled(FeatureKey.PAYMENTS, NOW);
        AggregateId actorId = AggregateId.newId();

        FeatureFlag updated = flag.withEnabled(false, actorId, NOW.plusSeconds(1));

        assertThat(updated.enabled()).isFalse();
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.updatedBy()).isEqualTo(actorId);
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void rejectsANullKey() {
        assertThatThrownBy(() -> new FeatureFlag(null, true, 0, NOW, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullUpdatedAt() {
        assertThatThrownBy(() -> new FeatureFlag(FeatureKey.STORAGE, true, 0, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
