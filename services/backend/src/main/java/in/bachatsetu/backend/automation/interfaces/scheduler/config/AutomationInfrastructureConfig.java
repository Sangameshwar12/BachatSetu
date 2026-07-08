package in.bachatsetu.backend.automation.interfaces.scheduler.config;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.interfaces.scheduler.adapter.SystemAutomationClockAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Supplies the Automation module's {@link ClockPort} and enables Spring's {@code @Scheduled} support
 * ({@link EnableScheduling}) — the first use of Spring scheduling in this codebase. Gated on {@code
 * bachatsetu.persistence.repositories.enabled}, matching every other module's infrastructure config:
 * if persistence is disabled there is nothing for a scheduled job to read or act on.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AutomationInfrastructureConfig {

    @Bean
    Clock automationClock() {
        return Clock.systemUTC();
    }

    @Bean
    ClockPort systemAutomationClockAdapter(Clock automationClock) {
        return new SystemAutomationClockAdapter(automationClock);
    }
}
