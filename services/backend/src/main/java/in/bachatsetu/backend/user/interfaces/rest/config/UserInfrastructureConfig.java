package in.bachatsetu.backend.user.interfaces.rest.config;

import in.bachatsetu.backend.user.application.port.ClockPort;
import in.bachatsetu.backend.user.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.user.application.port.TransactionPort;
import in.bachatsetu.backend.user.interfaces.rest.adapter.ApplicationEventUserEventPublisherAdapter;
import in.bachatsetu.backend.user.interfaces.rest.adapter.SpringUserTransactionAdapter;
import in.bachatsetu.backend.user.interfaces.rest.adapter.SystemUserClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the user module's outbound port adapters.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} for the same reason documented on
 * {@code NotificationInfrastructureConfig}: {@code PlatformTransactionManager} is a deferred
 * auto-configuration bean, so a {@code @ConditionalOnBean} check on a regular, component-scanned
 * {@code @Configuration} class is not reliably ordered against it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class UserInfrastructureConfig {

    @Bean
    Clock userClock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate userTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemUserClockAdapter(Clock userClock) {
        return new SystemUserClockAdapter(userClock);
    }

    @Bean
    TransactionPort springUserTransactionAdapter(TransactionTemplate userTransactionTemplate) {
        return new SpringUserTransactionAdapter(userTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventUserEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventUserEventPublisherAdapter(publisher);
    }
}
