package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.infrastructure.auth.adapter.LoggingOtpSenderAdapter;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Log-only OTP delivery — the default for every Spring profile, including {@code prod}, until a
 * real provider is explicitly enabled. Active whenever {@code bachatsetu.sms.enabled}
 * ({@code SMS_PROVIDER_ENABLED}) is {@code false} or unset; {@link SmsInfrastructureConfig} is
 * active under the exact opposite condition, so precisely one {@link OtpSenderPort} bean exists
 * no matter which Spring profile is running or whether the flag was set at all.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "bachatsetu.sms", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalOtpSenderConfig {

    @Bean
    OtpSenderPort loggingOtpSenderAdapter(Clock authenticationClock) {
        return new LoggingOtpSenderAdapter(authenticationClock, UUID::randomUUID);
    }
}
