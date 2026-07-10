package in.bachatsetu.backend.admin.domain.configuration.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformLimitTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void ofStartsAtVersionZeroWithNoActor() {
        PlatformLimit limit = PlatformLimit.of(LimitKey.MAX_GROUPS, 100, NOW);

        assertThat(limit.key()).isEqualTo(LimitKey.MAX_GROUPS);
        assertThat(limit.value()).isEqualTo(100);
        assertThat(limit.version()).isZero();
        assertThat(limit.updatedBy()).isNull();
    }

    @Test
    void withValueIncrementsVersionAndRecordsActor() {
        PlatformLimit limit = PlatformLimit.of(LimitKey.MAX_MEMBERS, 100, NOW);
        AggregateId actorId = AggregateId.newId();

        PlatformLimit updated = limit.withValue(200, actorId, NOW.plusSeconds(1));

        assertThat(updated.value()).isEqualTo(200);
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.updatedBy()).isEqualTo(actorId);
    }

    @Test
    void rejectsAZeroOrNegativeValue() {
        assertThatThrownBy(() -> new PlatformLimit(LimitKey.MAX_UPLOADS, 0, 0, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlatformLimit(LimitKey.MAX_UPLOADS, -1, 0, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullKey() {
        assertThatThrownBy(() -> new PlatformLimit(null, 1, 0, NOW, null))
                .isInstanceOf(NullPointerException.class);
    }
}
