package in.bachatsetu.backend.member.interfaces.rest.config;

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
import in.bachatsetu.backend.member.application.usecase.CreateMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.JoinGroupParticipationUseCase;
import in.bachatsetu.backend.member.application.usecase.ListMemberProfilesUseCase;
import in.bachatsetu.backend.member.application.usecase.UpdateMemberProfileUseCase;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composes framework-free Member application services when all outbound ports exist. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({
    MemberRepository.class,
    MemberNumberGeneratorPort.class,
    DomainEventPublisherPort.class,
    ClockPort.class,
    TransactionPort.class
})
public class MemberApplicationConfig {

    @Bean
    public MemberApplicationMapper memberApplicationMapper() {
        return new MemberApplicationMapper();
    }

    @Bean
    public MemberAuthorizationService memberAuthorizationService() {
        return new MemberAuthorizationService();
    }

    @Bean
    public CreateMemberProfileUseCase createMemberProfileUseCase(
            MemberRepository repository,
            MemberNumberGeneratorPort numberGenerator,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            MemberApplicationMapper mapper) {
        return new CreateMemberProfileApplicationService(
                repository, numberGenerator, eventPublisher, clock, transaction, mapper);
    }

    @Bean
    public JoinGroupParticipationUseCase joinGroupParticipationUseCase(
            MemberRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            MemberApplicationMapper mapper) {
        return new JoinGroupParticipationApplicationService(repository, eventPublisher, clock, transaction, mapper);
    }

    @Bean
    public GetMemberProfileUseCase getMemberProfileUseCase(
            MemberRepository repository,
            TransactionPort transaction,
            MemberApplicationMapper mapper,
            MemberAuthorizationService authorization) {
        return new GetMemberProfileApplicationService(repository, transaction, mapper, authorization);
    }

    @Bean
    public ListMemberProfilesUseCase listMemberProfilesUseCase(
            MemberRepository repository,
            TransactionPort transaction,
            MemberApplicationMapper mapper) {
        return new ListMemberProfilesApplicationService(repository, transaction, mapper);
    }

    @Bean
    public UpdateMemberProfileUseCase updateMemberProfileUseCase(
            MemberRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            MemberApplicationMapper mapper,
            MemberAuthorizationService authorization) {
        return new UpdateMemberProfileApplicationService(
                repository, eventPublisher, clock, transaction, mapper, authorization);
    }
}
