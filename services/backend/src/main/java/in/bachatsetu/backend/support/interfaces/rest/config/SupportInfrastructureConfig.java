package in.bachatsetu.backend.support.interfaces.rest.config;

import in.bachatsetu.backend.support.application.port.ClockPort;
import in.bachatsetu.backend.support.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.interfaces.rest.adapter.ApplicationEventSupportEventPublisherAdapter;
import in.bachatsetu.backend.support.interfaces.rest.adapter.SpringSupportTransactionAdapter;
import in.bachatsetu.backend.support.interfaces.rest.adapter.SystemSupportClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Support module's outbound port adapters.
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
public class SupportInfrastructureConfig {

    @Bean
    Clock supportClock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate supportTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemSupportClockAdapter(Clock supportClock) {
        return new SystemSupportClockAdapter(supportClock);
    }

    @Bean
    TransactionPort springSupportTransactionAdapter(TransactionTemplate supportTransactionTemplate) {
        return new SpringSupportTransactionAdapter(supportTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventSupportEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventSupportEventPublisherAdapter(publisher);
    }
}
