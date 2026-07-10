package in.bachatsetu.backend.admin.interfaces.rest.config;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.service.DisableUserApplicationService;
import in.bachatsetu.backend.admin.application.service.EnableUserApplicationService;
import in.bachatsetu.backend.admin.application.service.GetPlatformStatisticsApplicationService;
import in.bachatsetu.backend.admin.application.service.ListPlatformGroupsApplicationService;
import in.bachatsetu.backend.admin.application.service.ListPlatformTenantsApplicationService;
import in.bachatsetu.backend.admin.application.service.ListPlatformUsersApplicationService;
import in.bachatsetu.backend.admin.application.usecase.DisableUserUseCase;
import in.bachatsetu.backend.admin.application.usecase.EnableUserUseCase;
import in.bachatsetu.backend.admin.application.usecase.GetPlatformStatisticsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformGroupsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformTenantsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformUsersUseCase;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformStatisticsRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformTenantRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Admin application services.
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
public class AdminApplicationConfig {

    @Bean
    public AdminApplicationMapper adminApplicationMapper() {
        return new AdminApplicationMapper();
    }

    @Bean
    public GetPlatformStatisticsUseCase getPlatformStatisticsUseCase(
            PlatformStatisticsRepository repository, TransactionPort transaction, AdminApplicationMapper mapper) {
        return new GetPlatformStatisticsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListPlatformUsersUseCase listPlatformUsersUseCase(
            PlatformUserRepository repository, TransactionPort transaction, AdminApplicationMapper mapper) {
        return new ListPlatformUsersApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListPlatformGroupsUseCase listPlatformGroupsUseCase(
            PlatformGroupRepository repository, TransactionPort transaction, AdminApplicationMapper mapper) {
        return new ListPlatformGroupsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListPlatformTenantsUseCase listPlatformTenantsUseCase(
            PlatformTenantRepository repository, TransactionPort transaction, AdminApplicationMapper mapper) {
        return new ListPlatformTenantsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public EnableUserUseCase enableUserUseCase(
            PlatformUserRepository repository, ClockPort clock, TransactionPort transaction,
            AdminApplicationMapper mapper) {
        return new EnableUserApplicationService(repository, clock, transaction, mapper);
    }

    @Bean
    public DisableUserUseCase disableUserUseCase(
            PlatformUserRepository repository, ClockPort clock, TransactionPort transaction,
            AdminApplicationMapper mapper) {
        return new DisableUserApplicationService(repository, clock, transaction, mapper);
    }
}
