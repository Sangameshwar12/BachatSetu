package in.bachatsetu.backend.infrastructure.group.config;

import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.domain.service.GroupCodeGenerator;
import in.bachatsetu.backend.infrastructure.group.adapter.ApplicationEventDomainEventPublisherAdapter;
import in.bachatsetu.backend.infrastructure.group.adapter.SavingsGroupCodeGeneratorAdapter;
import in.bachatsetu.backend.infrastructure.group.adapter.SpringGroupTransactionAdapter;
import in.bachatsetu.backend.infrastructure.group.adapter.SystemGroupClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Composes the Savings Group outbound port adapters that Sprint 9.3 deferred. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(PlatformTransactionManager.class)
public class GroupInfrastructureConfig {

    @Bean
    Clock groupClock() {
        return Clock.systemUTC();
    }

    @Bean
    GroupCodeGenerator groupCodeGenerator() {
        return new GroupCodeGenerator();
    }

    @Bean
    TransactionTemplate groupTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemGroupClockAdapter(Clock groupClock) {
        return new SystemGroupClockAdapter(groupClock);
    }

    @Bean
    TransactionPort springGroupTransactionAdapter(TransactionTemplate groupTransactionTemplate) {
        return new SpringGroupTransactionAdapter(groupTransactionTemplate);
    }

    @Bean
    GroupCodeGeneratorPort savingsGroupCodeGeneratorAdapter(GroupCodeGenerator groupCodeGenerator) {
        return new SavingsGroupCodeGeneratorAdapter(groupCodeGenerator);
    }

    @Bean
    DomainEventPublisherPort applicationEventDomainEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventDomainEventPublisherAdapter(publisher);
    }
}
