package in.bachatsetu.backend.admin.interfaces.rest.config;

import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.interfaces.rest.adapter.SpringAdminTransactionAdapter;
import in.bachatsetu.backend.admin.interfaces.rest.adapter.SystemAdminClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composes the Admin outbound port adapters: the standard Clock/Transaction pair.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's
 * infrastructure config, for the same non-deterministic-condition-evaluation-order reason documented on
 * {@code PaymentGatewayInfrastructureConfig}. {@link AdminProperties} itself is bound separately, in {@link
 * AdminPropertiesConfig}, since it must be unconditionally available.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AdminInfrastructureConfig {

    @Bean
    Clock adminClock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate adminTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ClockPort systemAdminClockAdapter(Clock adminClock) {
        return new SystemAdminClockAdapter(adminClock);
    }

    @Bean
    TransactionPort springAdminTransactionAdapter(TransactionTemplate adminTransactionTemplate) {
        return new SpringAdminTransactionAdapter(adminTransactionTemplate);
    }
}
