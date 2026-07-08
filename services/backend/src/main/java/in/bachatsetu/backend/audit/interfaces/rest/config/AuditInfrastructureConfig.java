package in.bachatsetu.backend.audit.interfaces.rest.config;

import in.bachatsetu.backend.audit.application.port.AuditPublisherPort;
import in.bachatsetu.backend.audit.application.port.ClockPort;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.interfaces.rest.adapter.ApplicationEventAuditPublisherAdapter;
import in.bachatsetu.backend.audit.interfaces.rest.adapter.SpringAuditTransactionAdapter;
import in.bachatsetu.backend.audit.interfaces.rest.adapter.SystemAuditClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Audit outbound port adapters: the standard Clock/Transaction pair, plus an
 * {@link AuditPublisherPort} adapter publishing every created entry through Spring's application event bus.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's
 * infrastructure config, for the same non-deterministic-condition-evaluation-order reason documented on
 * {@code PaymentGatewayInfrastructureConfig}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuditInfrastructureConfig {

    @Bean
    Clock auditClock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate auditTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemAuditClockAdapter(Clock auditClock) {
        return new SystemAuditClockAdapter(auditClock);
    }

    @Bean
    TransactionPort springAuditTransactionAdapter(TransactionTemplate auditTransactionTemplate) {
        return new SpringAuditTransactionAdapter(auditTransactionTemplate);
    }

    @Bean
    AuditPublisherPort applicationEventAuditPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventAuditPublisherAdapter(publisher);
    }
}
