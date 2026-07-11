package in.bachatsetu.backend.infrastructure.email.config;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.infrastructure.email.adapter.LoggingEmailSenderAdapter;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Log-only email delivery for interactive development ({@code local}) and the automated test
 * suite ({@code test}) — neither needs, nor should require, live email provider credentials.
 * Every other profile ({@code dev}, {@code prod}) uses {@link
 * in.bachatsetu.backend.infrastructure.email.RetryingEmailSenderAdapter} instead, wired by {@link
 * EmailInfrastructureConfig}.
 */
@Configuration(proxyBeanMethods = false)
@Profile({"local", "test"})
public class LocalEmailSenderConfig {

    @Bean
    EmailSenderPort loggingEmailSenderAdapter(Clock authenticationClock) {
        return new LoggingEmailSenderAdapter(authenticationClock, UUID::randomUUID);
    }
}
