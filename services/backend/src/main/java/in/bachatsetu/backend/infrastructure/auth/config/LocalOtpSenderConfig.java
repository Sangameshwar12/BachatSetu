package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.infrastructure.auth.adapter.LoggingOtpSenderAdapter;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Log-only OTP delivery for interactive development ({@code local}) and the automated test
 * suite ({@code test}) — neither needs, nor should require, live SMS provider credentials. Every
 * other profile ({@code dev}, {@code prod}) uses {@link
 * in.bachatsetu.backend.infrastructure.auth.sms.SmsOtpSenderAdapter} instead, wired by {@link
 * SmsInfrastructureConfig}.
 */
@Configuration(proxyBeanMethods = false)
@Profile({"local", "test", "prod"})
public class LocalOtpSenderConfig {

    @Bean
    OtpSenderPort loggingOtpSenderAdapter(Clock authenticationClock) {
        return new LoggingOtpSenderAdapter(authenticationClock, UUID::randomUUID);
    }
}
