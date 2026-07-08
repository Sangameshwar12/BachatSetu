package in.bachatsetu.backend.group.interfaces.rest.config;

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
import in.bachatsetu.backend.group.application.usecase.ActivateGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.CloseGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.GetSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.JoinGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.ListSavingsGroupsUseCase;
import in.bachatsetu.backend.group.application.usecase.RemoveMemberUseCase;
import in.bachatsetu.backend.group.application.usecase.SuspendGroupUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Savings Group application services when all outbound ports exist.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} rather than a
 * cross-configuration-class {@code @ConditionalOnBean} check: regular (non-auto-configuration)
 * {@code @Configuration} classes discovered by component scanning have no guaranteed processing
 * order relative to one another, so a class-level {@code @ConditionalOnBean} referencing ports
 * defined by {@code GroupInfrastructureConfig} was evaluated non-deterministically and could skip
 * this configuration even when every required port was actually present.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SavingsGroupApplicationConfig {

    @Bean
    public SavingsGroupApplicationMapper savingsGroupApplicationMapper() {
        return new SavingsGroupApplicationMapper();
    }

    @Bean
    public GroupAuthorizationService groupAuthorizationService() {
        return new GroupAuthorizationService();
    }

    @Bean
    public CreateSavingsGroupUseCase createSavingsGroupUseCase(
            SavingsGroupRepository repository,
            GroupCodeGeneratorPort codeGenerator,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper) {
        return new CreateSavingsGroupApplicationService(
                repository, codeGenerator, eventPublisher, clock, transaction, mapper);
    }

    @Bean
    public GetSavingsGroupUseCase getSavingsGroupUseCase(
            SavingsGroupRepository repository,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper) {
        return new GetSavingsGroupApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListSavingsGroupsUseCase listSavingsGroupsUseCase(
            SavingsGroupRepository repository,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper) {
        return new ListSavingsGroupsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ActivateGroupUseCase activateGroupUseCase(
            SavingsGroupRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper,
            GroupAuthorizationService authorization) {
        return new ActivateGroupApplicationService(
                repository, eventPublisher, clock, transaction, mapper, authorization);
    }

    @Bean
    public SuspendGroupUseCase suspendGroupUseCase(
            SavingsGroupRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper,
            GroupAuthorizationService authorization) {
        return new SuspendGroupApplicationService(
                repository, eventPublisher, clock, transaction, mapper, authorization);
    }

    @Bean
    public CloseGroupUseCase closeGroupUseCase(
            SavingsGroupRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper,
            GroupAuthorizationService authorization) {
        return new CloseGroupApplicationService(
                repository, eventPublisher, clock, transaction, mapper, authorization);
    }

    @Bean
    public JoinGroupUseCase joinGroupUseCase(
            SavingsGroupRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper,
            GroupAuthorizationService authorization) {
        return new JoinGroupApplicationService(
                repository, eventPublisher, clock, transaction, mapper, authorization);
    }

    @Bean
    public RemoveMemberUseCase removeMemberUseCase(
            SavingsGroupRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            SavingsGroupApplicationMapper mapper,
            GroupAuthorizationService authorization) {
        return new RemoveMemberApplicationService(
                repository, eventPublisher, clock, transaction, mapper, authorization);
    }
}
