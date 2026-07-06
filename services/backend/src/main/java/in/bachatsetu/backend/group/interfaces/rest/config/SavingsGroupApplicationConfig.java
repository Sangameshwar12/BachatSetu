package in.bachatsetu.backend.group.interfaces.rest.config;

import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.application.service.CreateSavingsGroupApplicationService;
import in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composes framework-free Savings Group application services when all outbound ports exist. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({
    SavingsGroupRepository.class,
    GroupCodeGeneratorPort.class,
    DomainEventPublisherPort.class,
    ClockPort.class,
    TransactionPort.class
})
public class SavingsGroupApplicationConfig {

    @Bean
    public SavingsGroupApplicationMapper savingsGroupApplicationMapper() {
        return new SavingsGroupApplicationMapper();
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
}
