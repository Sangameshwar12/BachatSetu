package in.bachatsetu.backend.infrastructure.email.config;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.infrastructure.email.adapter.LoggingEmailSenderAdapter;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LocalEmailSenderConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LocalEmailSenderConfig.class)
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void wiresTheLoggingSenderWhenEmailIsNotExplicitlyEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBean(EmailSenderPort.class).isInstanceOf(LoggingEmailSenderAdapter.class);
        });
    }

    @Test
    void wiresTheLoggingSenderUnderProdTooWhenEmailIsNotExplicitlyEnabled() {
        // Whether the logging sender is active is a deployment-mode switch
        // (bachatsetu.email.enabled), not a Spring-profile one — an MVP deployment legitimately
        // runs the "prod" profile while still wanting log-only email delivery.
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(EmailSenderPort.class).isInstanceOf(LoggingEmailSenderAdapter.class);
                });
    }

    @Test
    void doesNotExposeTheLoggingSenderWhenEmailIsExplicitlyEnabled() {
        // The local-config side of the exactly-one-EmailSenderPort-bean contract — see
        // EmailInfrastructureConfigTest for the mirror-image assertion.
        contextRunner
                .withPropertyValues("bachatsetu.email.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(EmailSenderPort.class);
                });
    }
}
