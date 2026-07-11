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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Savings Group outbound port adapters that Sprint 9.3 deferred.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} rather than
 * {@code @ConditionalOnBean(PlatformTransactionManager.class)}: {@code PlatformTransactionManager}
 * is registered by a Spring Boot auto-configuration, which is processed as a deferred import
 * after every regular, component-scanned {@code @Configuration} class has already had its
 * class-level conditions evaluated — so the bean-presence check was never guaranteed to see it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
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
    DomainEventPublisherPort groupApplicationEventDomainEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventDomainEventPublisherAdapter(publisher);
    }
}
