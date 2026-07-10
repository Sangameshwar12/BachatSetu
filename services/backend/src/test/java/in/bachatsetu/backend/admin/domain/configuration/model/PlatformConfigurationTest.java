package in.bachatsetu.backend.admin.domain.configuration.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformConfigurationTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void updateAppliesSettingsAndIncrementsVersion() {
        PlatformConfiguration configuration = defaultConfiguration();
        AggregateId actorId = AggregateId.newId();

        configuration.update(
                "HINDI", 600, "AWS_S3", "STRIPE", 5, 20_000_000L, 200, 40, false, null, null, null, actorId,
                NOW.plusSeconds(1));

        assertThat(configuration.defaultLanguage()).isEqualTo("HINDI");
        assertThat(configuration.otpExpirySeconds()).isEqualTo(600);
        assertThat(configuration.version()).isEqualTo(1);
        assertThat(configuration.updatedBy()).isEqualTo(actorId);
    }

    @Test
    void rejectsABlankDefaultLanguage() {
        PlatformConfiguration configuration = defaultConfiguration();
        assertThatThrownBy(() -> configuration.update(
                " ", 300, "LOCAL", "RAZORPAY", 3, 1000L, 10, 5, false, null, null, null, AggregateId.newId(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANonPositiveOtpExpiry() {
        PlatformConfiguration configuration = defaultConfiguration();
        assertThatThrownBy(() -> configuration.update(
                "ENGLISH", 0, "LOCAL", "RAZORPAY", 3, 1000L, 10, 5, false, null, null, null, AggregateId.newId(),
                NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMaintenanceEndBeforeStart() {
        PlatformConfiguration configuration = defaultConfiguration();
        Instant start = NOW.plusSeconds(100);
        Instant end = NOW;
        assertThatThrownBy(() -> configuration.update(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 1000L, 10, 5, true, "msg", start, end,
                AggregateId.newId(), NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maintenanceIsInactiveWhenDisabled() {
        PlatformConfiguration configuration = defaultConfiguration();

        assertThat(configuration.isMaintenanceActiveAt(NOW)).isFalse();
    }

    @Test
    void maintenanceIsActiveWhenEnabledWithNoWindow() {
        PlatformConfiguration configuration = defaultConfiguration();
        configuration.update(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 1000L, 10, 5, true, "down for maintenance", null, null,
                AggregateId.newId(), NOW);

        assertThat(configuration.isMaintenanceActiveAt(NOW)).isTrue();
        assertThat(configuration.isMaintenanceActiveAt(NOW.plusSeconds(10_000))).isTrue();
    }

    @Test
    void maintenanceRespectsAScheduledWindow() {
        PlatformConfiguration configuration = defaultConfiguration();
        Instant start = NOW.plusSeconds(100);
        Instant end = NOW.plusSeconds(200);
        configuration.update(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 1000L, 10, 5, true, "scheduled", start, end,
                AggregateId.newId(), NOW);

        assertThat(configuration.isMaintenanceActiveAt(NOW)).isFalse();
        assertThat(configuration.isMaintenanceActiveAt(start.plusSeconds(1))).isTrue();
        assertThat(configuration.isMaintenanceActiveAt(end)).isTrue();
        assertThat(configuration.isMaintenanceActiveAt(end.plusSeconds(1))).isFalse();
    }

    private PlatformConfiguration defaultConfiguration() {
        return PlatformConfiguration.of(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null, 0, NOW, null);
    }
}
