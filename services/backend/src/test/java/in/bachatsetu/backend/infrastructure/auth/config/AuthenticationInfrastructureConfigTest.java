package in.bachatsetu.backend.infrastructure.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.infrastructure.auth.adapter.BCryptHashingAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.LoggingOtpSenderAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.SecureRandomGeneratorAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.SystemClockAdapter;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthenticationInfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    AuthenticationInfrastructureConfig.class,
                    LocalOtpSenderConfig.class)
            .withPropertyValues(
                    "bachatsetu.authentication.otp-validity=5m",
                    "bachatsetu.authentication.resend-limit=3",
                    "bachatsetu.authentication.verify-limit=5",
                    "bachatsetu.authentication.hash-strength=10");

    @Test
    void bindsPropertiesAndWiresEveryAdapterForLocalProfile() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(Clock.class).extracting(Clock::getZone).isEqualTo(java.time.ZoneOffset.UTC);
                    assertThat(context).getBean(SecureRandom.class).isNotNull();
                    assertThat(context.getBean(BCryptPasswordEncoder.class).encode("123456"))
                            .startsWith("$2a$10$");
                    assertThat(context).getBean(ClockPort.class).isInstanceOf(SystemClockAdapter.class);
                    assertThat(context).getBean(RandomGeneratorPort.class)
                            .isInstanceOf(SecureRandomGeneratorAdapter.class);
                    assertThat(context).getBean(HashingPort.class).isInstanceOf(BCryptHashingAdapter.class);
                    assertThat(context).getBean(OtpSenderPort.class).isInstanceOf(LoggingOtpSenderAdapter.class);

                    AuthenticationProperties properties = context.getBean(AuthenticationProperties.class);
                    assertThat(properties.otpValidity()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(properties.resendLimit()).isEqualTo(3);
                    assertThat(properties.verifyLimit()).isEqualTo(5);
                    assertThat(properties.hashStrength()).isEqualTo(10);
                });
    }

    @Test
    void wiresTheLoggingSenderUnderProdTooWhenSmsIsNotExplicitlyEnabled() {
        // Whether the logging sender is active is a deployment-mode switch
        // (bachatsetu.sms.enabled), not a Spring-profile one — an MVP deployment legitimately
        // runs the "prod" profile while still wanting log-only OTP delivery. See
        // SmsInfrastructureConfigTest for the mirror-image assertion once bachatsetu.sms.enabled
        // is true.
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(OtpSenderPort.class).isInstanceOf(LoggingOtpSenderAdapter.class);
                });
    }

    @Test
    void doesNotExposeTheLoggingSenderWhenSmsIsExplicitlyEnabled() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues("bachatsetu.sms.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(OtpSenderPort.class);
                });
    }
}
