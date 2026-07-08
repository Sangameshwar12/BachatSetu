package in.bachatsetu.backend.payment.interfaces.rest.config;

import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.interfaces.rest.adapter.ApplicationEventPaymentEventPublisherAdapter;
import in.bachatsetu.backend.payment.interfaces.rest.adapter.SpringPaymentTransactionAdapter;
import in.bachatsetu.backend.payment.interfaces.rest.adapter.SystemPaymentClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Payment outbound port adapters backing the application layer.
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
public class PaymentInfrastructureConfig {

    @Bean
    Clock paymentClock() {
        return Clock.systemUTC();
    }

    @Bean
    PaymentFactory paymentFactory(Clock paymentClock) {
        return new PaymentFactory(paymentClock);
    }

    @Bean
    TransactionTemplate paymentTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemPaymentClockAdapter(Clock paymentClock) {
        return new SystemPaymentClockAdapter(paymentClock);
    }

    @Bean
    TransactionPort springPaymentTransactionAdapter(TransactionTemplate paymentTransactionTemplate) {
        return new SpringPaymentTransactionAdapter(paymentTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventPaymentEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventPaymentEventPublisherAdapter(publisher);
    }
}
