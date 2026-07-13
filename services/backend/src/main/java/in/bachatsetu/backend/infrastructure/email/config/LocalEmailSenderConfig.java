package in.bachatsetu.backend.infrastructure.email.config;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.infrastructure.email.adapter.LoggingEmailSenderAdapter;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Log-only email delivery — the default for every Spring profile, including {@code prod}, until
 * a real provider is explicitly enabled. Active whenever {@code bachatsetu.email.enabled}
 * ({@code EMAIL_PROVIDER_ENABLED}) is {@code false} or unset; {@link EmailInfrastructureConfig}
 * is active under the exact opposite condition, so precisely one {@link EmailSenderPort} bean
 * exists no matter which Spring profile is running or whether the flag was set at all — unlike
 * gating on Spring profiles, which made it possible for both configurations to be active
 * together (this class previously listed {@code prod} directly, alongside {@code dev}/{@code
 * prod} on {@link EmailInfrastructureConfig}, producing two {@code EmailSenderPort} beans).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "bachatsetu.email", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalEmailSenderConfig {

    @Bean
    EmailSenderPort emailInfrastructureLoggingEmailSenderAdapter(Clock authenticationClock) {
        return new LoggingEmailSenderAdapter(authenticationClock, UUID::randomUUID);
    }
}
