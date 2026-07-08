package in.bachatsetu.backend.draw.interfaces.rest.config;

import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.domain.factory.DrawFactory;
import in.bachatsetu.backend.draw.interfaces.rest.adapter.ApplicationEventDrawEventPublisherAdapter;
import in.bachatsetu.backend.draw.interfaces.rest.adapter.SpringDrawTransactionAdapter;
import in.bachatsetu.backend.draw.interfaces.rest.adapter.SystemDrawClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Draw outbound port adapters backing the application layer.
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
public class DrawInfrastructureConfig {

    @Bean
    Clock drawClock() {
        return Clock.systemUTC();
    }

    @Bean
    DrawFactory drawFactory(Clock drawClock) {
        return new DrawFactory(drawClock);
    }

    @Bean
    TransactionTemplate drawTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemDrawClockAdapter(Clock drawClock) {
        return new SystemDrawClockAdapter(drawClock);
    }

    @Bean
    TransactionPort springDrawTransactionAdapter(TransactionTemplate drawTransactionTemplate) {
        return new SpringDrawTransactionAdapter(drawTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventDrawEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventDrawEventPublisherAdapter(publisher);
    }
}
