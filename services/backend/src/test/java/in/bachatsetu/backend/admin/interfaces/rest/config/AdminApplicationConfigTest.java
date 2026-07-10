package in.bachatsetu.backend.admin.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.service.DisableUserApplicationService;
import in.bachatsetu.backend.admin.application.service.EnableUserApplicationService;
import in.bachatsetu.backend.admin.application.service.GetPlatformStatisticsApplicationService;
import in.bachatsetu.backend.admin.application.service.ListPlatformGroupsApplicationService;
import in.bachatsetu.backend.admin.application.service.ListPlatformTenantsApplicationService;
import in.bachatsetu.backend.admin.application.service.ListPlatformUsersApplicationService;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformStatisticsRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformTenantRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformUserRepository;
import org.junit.jupiter.api.Test;

class AdminApplicationConfigTest {

    private final AdminApplicationConfig config = new AdminApplicationConfig();
    private final AdminApplicationMapper mapper = config.adminApplicationMapper();
    private final PlatformUserRepository userRepository = mock(PlatformUserRepository.class);
    private final PlatformGroupRepository groupRepository = mock(PlatformGroupRepository.class);
    private final PlatformTenantRepository tenantRepository = mock(PlatformTenantRepository.class);
    private final PlatformStatisticsRepository statisticsRepository = mock(PlatformStatisticsRepository.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);

    @Test
    void composesGetPlatformStatisticsUseCase() {
        assertThat(config.getPlatformStatisticsUseCase(statisticsRepository, transaction, mapper))
                .isInstanceOf(GetPlatformStatisticsApplicationService.class);
    }

    @Test
    void composesListPlatformUsersUseCase() {
        assertThat(config.listPlatformUsersUseCase(userRepository, transaction, mapper))
                .isInstanceOf(ListPlatformUsersApplicationService.class);
    }

    @Test
    void composesListPlatformGroupsUseCase() {
        assertThat(config.listPlatformGroupsUseCase(groupRepository, transaction, mapper))
                .isInstanceOf(ListPlatformGroupsApplicationService.class);
    }

    @Test
    void composesListPlatformTenantsUseCase() {
        assertThat(config.listPlatformTenantsUseCase(tenantRepository, transaction, mapper))
                .isInstanceOf(ListPlatformTenantsApplicationService.class);
    }

    @Test
    void composesEnableUserUseCase() {
        assertThat(config.enableUserUseCase(userRepository, clock, transaction, mapper))
                .isInstanceOf(EnableUserApplicationService.class);
    }

    @Test
    void composesDisableUserUseCase() {
        assertThat(config.disableUserUseCase(userRepository, clock, transaction, mapper))
                .isInstanceOf(DisableUserApplicationService.class);
    }
}
