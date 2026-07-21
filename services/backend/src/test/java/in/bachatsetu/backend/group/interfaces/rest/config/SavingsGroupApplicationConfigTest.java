package in.bachatsetu.backend.group.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.application.security.GroupAuthorizationService;
import in.bachatsetu.backend.group.application.service.ActivateGroupApplicationService;
import in.bachatsetu.backend.group.application.service.CloseGroupApplicationService;
import in.bachatsetu.backend.group.application.service.CreateSavingsGroupApplicationService;
import in.bachatsetu.backend.group.application.service.GetSavingsGroupApplicationService;
import in.bachatsetu.backend.group.application.service.JoinGroupApplicationService;
import in.bachatsetu.backend.group.application.service.ListSavingsGroupsApplicationService;
import in.bachatsetu.backend.group.application.service.RemoveMemberApplicationService;
import in.bachatsetu.backend.group.application.service.SuspendGroupApplicationService;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import org.junit.jupiter.api.Test;

class SavingsGroupApplicationConfigTest {

    private final SavingsGroupApplicationConfig config = new SavingsGroupApplicationConfig();
    private final SavingsGroupApplicationMapper mapper = config.savingsGroupApplicationMapper();
    private final SavingsGroupRepository repository = mock(SavingsGroupRepository.class);
    private final GroupCodeGeneratorPort codeGenerator = mock(GroupCodeGeneratorPort.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    @Test
    void composesCreateSavingsGroupUseCase() {
        assertThat(config.createSavingsGroupUseCase(repository, codeGenerator, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(CreateSavingsGroupApplicationService.class);
    }

    @Test
    void composesGetAndListUseCases() {
        assertThat(config.getSavingsGroupUseCase(repository, transaction, mapper, userRepository))
                .isInstanceOf(GetSavingsGroupApplicationService.class);
        assertThat(config.listSavingsGroupsUseCase(repository, transaction, mapper))
                .isInstanceOf(ListSavingsGroupsApplicationService.class);
    }

    @Test
    void composesGroupAuthorizationService() {
        assertThat(config.groupAuthorizationService()).isInstanceOf(GroupAuthorizationService.class);
    }

    @Test
    void composesLifecycleUseCases() {
        GroupAuthorizationService authorization = config.groupAuthorizationService();

        assertThat(config.activateGroupUseCase(repository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(ActivateGroupApplicationService.class);
        assertThat(config.suspendGroupUseCase(repository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(SuspendGroupApplicationService.class);
        assertThat(config.closeGroupUseCase(repository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(CloseGroupApplicationService.class);
    }

    @Test
    void composesMembershipUseCases() {
        GroupAuthorizationService authorization = config.groupAuthorizationService();

        assertThat(config.joinGroupUseCase(repository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(JoinGroupApplicationService.class);
        assertThat(config.removeMemberUseCase(repository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(RemoveMemberApplicationService.class);
    }
}
