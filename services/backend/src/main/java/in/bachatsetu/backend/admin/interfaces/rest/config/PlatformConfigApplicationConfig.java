package in.bachatsetu.backend.admin.interfaces.rest.config;

import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.service.FeatureFlagQueryService;
import in.bachatsetu.backend.admin.application.configuration.service.GetConfigurationApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.GetFeatureFlagsApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.GetSystemLimitsApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.MaintenanceStatusQueryService;
import in.bachatsetu.backend.admin.application.configuration.service.UpdateConfigurationApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.UpdateFeatureFlagsApplicationService;
import in.bachatsetu.backend.admin.application.configuration.service.UpdateSystemLimitsApplicationService;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetConfigurationUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetFeatureFlagsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetSystemLimitsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateConfigurationUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateFeatureFlagsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateSystemLimitsUseCase;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformLimitRepository;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Platform Configuration application services — additive to the Admin module,
 * reusing its existing {@link TransactionPort}/{@link ClockPort} adapters.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's application
 * config.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PlatformConfigApplicationConfig {

    @Bean
    public PlatformConfigApplicationMapper platformConfigApplicationMapper() {
        return new PlatformConfigApplicationMapper();
    }

    @Bean
    public GetConfigurationUseCase getConfigurationUseCase(
            PlatformConfigurationRepository repository, TransactionPort transaction,
            PlatformConfigApplicationMapper mapper) {
        return new GetConfigurationApplicationService(repository, transaction, mapper);
    }

    @Bean
    public UpdateConfigurationUseCase updateConfigurationUseCase(
            PlatformConfigurationRepository repository, TransactionPort transaction,
            PlatformConfigApplicationMapper mapper, CreateAuditEntryUseCase createAuditEntry, ClockPort clock) {
        return new UpdateConfigurationApplicationService(repository, transaction, mapper, createAuditEntry, clock);
    }

    @Bean
    public GetFeatureFlagsUseCase getFeatureFlagsUseCase(
            FeatureFlagRepository repository, TransactionPort transaction, PlatformConfigApplicationMapper mapper) {
        return new GetFeatureFlagsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public UpdateFeatureFlagsUseCase updateFeatureFlagsUseCase(
            FeatureFlagRepository repository, TransactionPort transaction, PlatformConfigApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry, ClockPort clock) {
        return new UpdateFeatureFlagsApplicationService(repository, transaction, mapper, createAuditEntry, clock);
    }

    @Bean
    public GetSystemLimitsUseCase getSystemLimitsUseCase(
            PlatformLimitRepository repository, TransactionPort transaction, PlatformConfigApplicationMapper mapper) {
        return new GetSystemLimitsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public UpdateSystemLimitsUseCase updateSystemLimitsUseCase(
            PlatformLimitRepository repository, TransactionPort transaction, PlatformConfigApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry, ClockPort clock) {
        return new UpdateSystemLimitsApplicationService(repository, transaction, mapper, createAuditEntry, clock);
    }

    @Bean
    public FeatureFlagQueryService featureFlagQueryService(FeatureFlagRepository repository) {
        return new FeatureFlagQueryService(repository);
    }

    @Bean
    public MaintenanceStatusQueryService maintenanceStatusQueryService(PlatformConfigurationRepository repository) {
        return new MaintenanceStatusQueryService(repository);
    }
}
