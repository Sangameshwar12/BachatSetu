package in.bachatsetu.backend.admin.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.service.FeatureFlagQueryService;
import in.bachatsetu.backend.admin.application.configuration.service.GetConfigurationApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.GetFeatureFlagsApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.GetSystemLimitsApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.MaintenanceStatusQueryService;
import in.bachatsetu.backend.admin.application.configuration.service.UpdateConfigurationApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.UpdateFeatureFlagsApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.UpdateSystemLimitsApplicationService;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformLimitRepository;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import org.junit.jupiter.api.Test;

class PlatformConfigApplicationConfigTest {

    private final PlatformConfigApplicationConfig config = new PlatformConfigApplicationConfig();
    private final PlatformConfigApplicationMapper mapper = config.platformConfigApplicationMapper();
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);

    @Test
    void composesGetConfigurationUseCase() {
        assertThat(config.getConfigurationUseCase(mock(PlatformConfigurationRepository.class), transaction, mapper))
                .isInstanceOf(GetConfigurationApplicationService.class);
    }

    @Test
    void composesUpdateConfigurationUseCase() {
        assertThat(config.updateConfigurationUseCase(
                        mock(PlatformConfigurationRepository.class), transaction, mapper, createAuditEntry, clock))
                .isInstanceOf(UpdateConfigurationApplicationService.class);
    }

    @Test
    void composesGetFeatureFlagsUseCase() {
        assertThat(config.getFeatureFlagsUseCase(mock(FeatureFlagRepository.class), transaction, mapper))
                .isInstanceOf(GetFeatureFlagsApplicationService.class);
    }

    @Test
    void composesUpdateFeatureFlagsUseCase() {
        assertThat(config.updateFeatureFlagsUseCase(
                        mock(FeatureFlagRepository.class), transaction, mapper, createAuditEntry, clock))
                .isInstanceOf(UpdateFeatureFlagsApplicationService.class);
    }

    @Test
    void composesGetSystemLimitsUseCase() {
        assertThat(config.getSystemLimitsUseCase(mock(PlatformLimitRepository.class), transaction, mapper))
                .isInstanceOf(GetSystemLimitsApplicationService.class);
    }

    @Test
    void composesUpdateSystemLimitsUseCase() {
        assertThat(config.updateSystemLimitsUseCase(
                        mock(PlatformLimitRepository.class), transaction, mapper, createAuditEntry, clock))
                .isInstanceOf(UpdateSystemLimitsApplicationService.class);
    }

    @Test
    void composesFeatureFlagQueryService() {
        assertThat(config.featureFlagQueryService(mock(FeatureFlagRepository.class)))
                .isInstanceOf(FeatureFlagQueryService.class);
    }

    @Test
    void composesMaintenanceStatusQueryService() {
        assertThat(config.maintenanceStatusQueryService(mock(PlatformConfigurationRepository.class)))
                .isInstanceOf(MaintenanceStatusQueryService.class);
    }
}
