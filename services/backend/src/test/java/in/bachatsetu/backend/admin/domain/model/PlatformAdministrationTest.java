package in.bachatsetu.backend.admin.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformAdministrationTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void decidesToEnableAUser() {
        AggregateId administratorId = AggregateId.newId();
        AggregateId userId = AggregateId.newId();
        PlatformAdministration administration = PlatformAdministration.actingAs(administratorId);

        PlatformUserStatusChange change = administration.enableUser(userId, NOW);

        assertThat(change.userId()).isEqualTo(userId);
        assertThat(change.targetStatus()).isEqualTo(PlatformUserStatus.ACTIVE);
        assertThat(change.administratorId()).isEqualTo(administratorId);
        assertThat(change.changedAt()).isEqualTo(NOW);
    }

    @Test
    void decidesToDisableAUser() {
        AggregateId administratorId = AggregateId.newId();
        AggregateId userId = AggregateId.newId();
        PlatformAdministration administration = PlatformAdministration.actingAs(administratorId);

        PlatformUserStatusChange change = administration.disableUser(userId, NOW);

        assertThat(change.targetStatus()).isEqualTo(PlatformUserStatus.DISABLED);
    }

    @Test
    void exposesTheActingAdministrator() {
        AggregateId administratorId = AggregateId.newId();

        assertThat(PlatformAdministration.actingAs(administratorId).administratorId()).isEqualTo(administratorId);
    }

    @Test
    void rejectsANullAdministrator() {
        assertThatThrownBy(() -> PlatformAdministration.actingAs(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullUserIdWhenEnabling() {
        PlatformAdministration administration = PlatformAdministration.actingAs(AggregateId.newId());

        assertThatThrownBy(() -> administration.enableUser(null, NOW)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullTimestampWhenDisabling() {
        PlatformAdministration administration = PlatformAdministration.actingAs(AggregateId.newId());

        assertThatThrownBy(() -> administration.disableUser(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
