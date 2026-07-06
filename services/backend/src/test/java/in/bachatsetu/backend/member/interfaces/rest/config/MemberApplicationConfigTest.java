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
    void composesGetMemberProfileUseCase() {
        assertThat(config.getMemberProfileUseCase(repository, transaction, mapper))
                .isInstanceOf(GetMemberProfileApplicationService.class);
    }
}
