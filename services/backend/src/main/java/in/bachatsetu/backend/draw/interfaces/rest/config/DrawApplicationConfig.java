package in.bachatsetu.backend.draw.interfaces.rest.config;

import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.application.service.CloseDrawApplicationService;
import in.bachatsetu.backend.draw.application.service.ConductDrawApplicationService;
import in.bachatsetu.backend.draw.application.service.CreateDrawApplicationService;
import in.bachatsetu.backend.draw.application.service.GetDrawApplicationService;
import in.bachatsetu.backend.draw.application.service.ListDrawsApplicationService;
import in.bachatsetu.backend.draw.application.usecase.CloseDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.CreateDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.GetDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ListDrawsUseCase;
import in.bachatsetu.backend.draw.domain.factory.DrawFactory;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composes framework-free Draw application services when all outbound ports exist. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({
    DrawRepository.class,
    DrawFactory.class,
    DomainEventPublisherPort.class,
    ClockPort.class,
    TransactionPort.class,
    SavingsGroupRepository.class
})
public class DrawApplicationConfig {

    @Bean
    public DrawApplicationMapper drawApplicationMapper() {
        return new DrawApplicationMapper();
    }

    @Bean
    public DrawAuthorizationService drawAuthorizationService() {
        return new DrawAuthorizationService();
    }

    @Bean
    public CreateDrawUseCase createDrawUseCase(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DrawFactory drawFactory,
            DomainEventPublisherPort eventPublisher,
            TransactionPort transaction,
            DrawApplicationMapper mapper,
            DrawAuthorizationService authorization) {
        return new CreateDrawApplicationService(
                repository, groupRepository, drawFactory, eventPublisher, transaction, mapper, authorization);
    }

    @Bean
    public GetDrawUseCase getDrawUseCase(
            DrawRepository repository,
            TransactionPort transaction,
            DrawApplicationMapper mapper) {
        return new GetDrawApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListDrawsUseCase listDrawsUseCase(
            DrawRepository repository,
            TransactionPort transaction,
            DrawApplicationMapper mapper) {
        return new ListDrawsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ConductDrawUseCase conductDrawUseCase(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            DrawApplicationMapper mapper,
            DrawAuthorizationService authorization) {
        return new ConductDrawApplicationService(
                repository, groupRepository, eventPublisher, clock, transaction, mapper, authorization);
    }

    @Bean
    public CloseDrawUseCase closeDrawUseCase(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            DrawApplicationMapper mapper,
            DrawAuthorizationService authorization) {
        return new CloseDrawApplicationService(
                repository, groupRepository, eventPublisher, clock, transaction, mapper, authorization);
    }
}
