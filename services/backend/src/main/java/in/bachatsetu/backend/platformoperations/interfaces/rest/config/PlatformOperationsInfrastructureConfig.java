package in.bachatsetu.backend.platformoperations.interfaces.rest.config;

import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.interfaces.rest.adapter.ApplicationEventPlatformOperationsEventPublisherAdapter;
import in.bachatsetu.backend.platformoperations.interfaces.rest.adapter.SpringPlatformOperationsTransactionAdapter;
import in.bachatsetu.backend.platformoperations.interfaces.rest.adapter.SystemPlatformOperationsClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Platform Operations module's outbound port adapters.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's
 * infrastructure config.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PlatformOperationsInfrastructureConfig {

    @Bean
    Clock platformOperationsClock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate platformOperationsTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemPlatformOperationsClockAdapter(Clock platformOperationsClock) {
        return new SystemPlatformOperationsClockAdapter(platformOperationsClock);
    }

    @Bean
    TransactionPort springPlatformOperationsTransactionAdapter(TransactionTemplate platformOperationsTransactionTemplate) {
        return new SpringPlatformOperationsTransactionAdapter(platformOperationsTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventPlatformOperationsEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventPlatformOperationsEventPublisherAdapter(publisher);
    }
}
