package in.bachatsetu.backend.member.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.ClockPort;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.port.MemberNumberGeneratorPort;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.application.service.CreateMemberProfileApplicationService;
import in.bachatsetu.backend.member.application.service.GetMemberProfileApplicationService;
import in.bachatsetu.backend.member.application.service.JoinGroupParticipationApplicationService;
import in.bachatsetu.backend.member.application.security.MemberAuthorizationService;
import in.bachatsetu.backend.member.application.service.ListMemberProfilesApplicationService;
import in.bachatsetu.backend.member.application.service.UpdateMemberProfileApplicationService;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import org.junit.jupiter.api.Test;

class MemberApplicationConfigTest {

    private final MemberApplicationConfig config = new MemberApplicationConfig();
    private final MemberApplicationMapper mapper = config.memberApplicationMapper();
    private final MemberRepository repository = mock(MemberRepository.class);
    private final MemberNumberGeneratorPort numberGenerator = mock(MemberNumberGeneratorPort.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);

    @Test
    void composesCreateMemberProfileUseCase() {
        assertThat(config.createMemberProfileUseCase(repository, numberGenerator, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(CreateMemberProfileApplicationService.class);
    }

    @Test
    void composesJoinGroupParticipationUseCase() {
        assertThat(config.joinGroupParticipationUseCase(repository, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(JoinGroupParticipationApplicationService.class);
    }

    @Test
    void composesMemberAuthorizationService() {
        assertThat(config.memberAuthorizationService()).isInstanceOf(MemberAuthorizationService.class);
    }

    @Test
    void composesGetMemberProfileUseCase() {
        MemberAuthorizationService authorization = config.memberAuthorizationService();
        assertThat(config.getMemberProfileUseCase(repository, transaction, mapper, authorization))
                .isInstanceOf(GetMemberProfileApplicationService.class);
    }

    @Test
    void composesListMemberProfilesUseCase() {
        assertThat(config.listMemberProfilesUseCase(repository, transaction, mapper))
                .isInstanceOf(ListMemberProfilesApplicationService.class);
    }

    @Test
    void composesUpdateMemberProfileUseCase() {
        MemberAuthorizationService authorization = config.memberAuthorizationService();
        assertThat(config.updateMemberProfileUseCase(repository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(UpdateMemberProfileApplicationService.class);
    }
}
