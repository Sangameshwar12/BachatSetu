package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.infrastructure.auth.adapter.LoggingOtpSenderAdapter;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalOtpSenderConfig {

    @Bean
    OtpSenderPort loggingOtpSenderAdapter(Clock authenticationClock) {
        return new LoggingOtpSenderAdapter(authenticationClock, UUID::randomUUID);
    }
}
