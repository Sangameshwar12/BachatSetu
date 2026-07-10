package in.bachatsetu.backend.admin.application.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MaintenanceStatusQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void reportsActiveMaintenanceWithMessage() {
        PlatformConfigurationRepository repository = mock(PlatformConfigurationRepository.class);
        when(repository.find()).thenReturn(PlatformConfiguration.of(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, true, "down for maintenance", null,
                null, 0, NOW, null));
        MaintenanceStatusQueryService service = new MaintenanceStatusQueryService(repository);

        MaintenanceStatus status = service.currentStatus(NOW);

        assertThat(status.active()).isTrue();
        assertThat(status.message()).isEqualTo("down for maintenance");
    }

    @Test
    void reportsInactiveWhenMaintenanceDisabled() {
        PlatformConfigurationRepository repository = mock(PlatformConfigurationRepository.class);
        when(repository.find()).thenReturn(PlatformConfiguration.of(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null, 0, NOW,
                null));
        MaintenanceStatusQueryService service = new MaintenanceStatusQueryService(repository);

        assertThat(service.currentStatus(NOW).active()).isFalse();
    }
}
