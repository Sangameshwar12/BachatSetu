package in.bachatsetu.backend.platformoperations.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.platformoperations.application.query.SystemHealthResult;
import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import in.bachatsetu.backend.platformoperations.domain.model.SystemRuntimeInfo;
import in.bachatsetu.backend.platformoperations.domain.port.DatabaseHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.NotificationHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.StorageHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.SystemRuntimeInfoPort;
import org.junit.jupiter.api.Test;

class GetSystemHealthApplicationServiceTest {

    private final DatabaseHealthPort databaseHealth = mock(DatabaseHealthPort.class);
    private final StorageHealthPort storageHealth = mock(StorageHealthPort.class);
    private final NotificationHealthPort notificationHealth = mock(NotificationHealthPort.class);
    private final SystemRuntimeInfoPort runtimeInfo = mock(SystemRuntimeInfoPort.class);
    private final GetSystemHealthApplicationService service =
            new GetSystemHealthApplicationService(databaseHealth, storageHealth, notificationHealth, runtimeInfo);

    @Test
    void composesEveryComponentAndRuntimeFact() {
        when(databaseHealth.check()).thenReturn(new ComponentHealth("database", HealthStatus.UP, "ok"));
        when(storageHealth.check()).thenReturn(new ComponentHealth("storage", HealthStatus.UP, "ok"));
        when(notificationHealth.check()).thenReturn(new ComponentHealth("notification", HealthStatus.DOWN, "down"));
        when(runtimeInfo.current()).thenReturn(new SystemRuntimeInfo(
                120, "21.0.9", "1.0.0", null, 1000, 2000, 4000, 5000, 10000));

        SystemHealthResult result = service.execute();

        assertThat(result.database().status()).isEqualTo(HealthStatus.UP);
        assertThat(result.notification().status()).isEqualTo(HealthStatus.DOWN);
        assertThat(result.uptimeSeconds()).isEqualTo(120);
        assertThat(result.javaVersion()).isEqualTo("21.0.9");
        assertThat(result.buildTimestamp()).isNull();
    }
}
