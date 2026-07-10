package in.bachatsetu.backend.admin.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformUserSummaryTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void recordsEveryField() {
        AggregateId userId = AggregateId.newId();
        AggregateId tenantId = AggregateId.newId();

        PlatformUserSummary summary = new PlatformUserSummary(
                userId, tenantId, "user@example.com", "+919876543210", "Asha", "Rao", PlatformUserStatus.ACTIVE,
                NOW);

        assertThat(summary.userId()).isEqualTo(userId);
        assertThat(summary.tenantId()).isEqualTo(tenantId);
        assertThat(summary.email()).isEqualTo("user@example.com");
        assertThat(summary.status()).isEqualTo(PlatformUserStatus.ACTIVE);
    }

    @Test
    void allowsNullContactFields() {
        PlatformUserSummary summary = new PlatformUserSummary(
                AggregateId.newId(), AggregateId.newId(), null, null, null, null, PlatformUserStatus.DISABLED, NOW);

        assertThat(summary.email()).isNull();
        assertThat(summary.phoneNumber()).isNull();
    }

    @Test
    void rejectsANullUserId() {
        assertThatThrownBy(() -> new PlatformUserSummary(
                        null, AggregateId.newId(), null, null, null, null, PlatformUserStatus.ACTIVE, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullStatus() {
        assertThatThrownBy(() -> new PlatformUserSummary(
                        AggregateId.newId(), AggregateId.newId(), null, null, null, null, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullCreatedAt() {
        assertThatThrownBy(() -> new PlatformUserSummary(
                        AggregateId.newId(), AggregateId.newId(), null, null, null, null, PlatformUserStatus.ACTIVE,
                        null))
                .isInstanceOf(NullPointerException.class);
    }
}
