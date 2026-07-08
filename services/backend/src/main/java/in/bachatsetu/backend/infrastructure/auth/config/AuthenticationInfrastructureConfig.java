package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.infrastructure.auth.adapter.ApplicationEventOtpPublisherAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.BCryptHashingAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.SecureRandomGeneratorAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.SystemClockAdapter;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptVersion;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationProperties.class)
public class AuthenticationInfrastructureConfig {

    @Bean
    Clock authenticationClock() {
        return Clock.systemUTC();
    }

    @Bean
    SecureRandom authenticationSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    BCryptPasswordEncoder otpPasswordEncoder(
            AuthenticationProperties properties,
            SecureRandom authenticationSecureRandom) {
        return new BCryptPasswordEncoder(
                BCryptVersion.$2A, properties.hashStrength(), authenticationSecureRandom);
    }

    @Bean
    ClockPort systemClockAdapter(Clock authenticationClock) {
        return new SystemClockAdapter(authenticationClock);
    }

    @Bean
    RandomGeneratorPort secureRandomGeneratorAdapter(SecureRandom authenticationSecureRandom) {
        return new SecureRandomGeneratorAdapter(authenticationSecureRandom);
    }

    @Bean
    HashingPort bcryptHashingAdapter(BCryptPasswordEncoder otpPasswordEncoder) {
        return new BCryptHashingAdapter(otpPasswordEncoder);
    }

    @Bean
    OtpEventPublisherPort applicationEventOtpPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventOtpPublisherAdapter(publisher);
    }
}
