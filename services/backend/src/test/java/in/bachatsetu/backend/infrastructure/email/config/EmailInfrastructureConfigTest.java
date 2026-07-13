package in.bachatsetu.backend.infrastructure.email.config;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.infrastructure.email.AwsSesEmailProviderClient;
import in.bachatsetu.backend.infrastructure.email.EmailProviderClient;
import in.bachatsetu.backend.infrastructure.email.EmailProviderHealthTracker;
import in.bachatsetu.backend.infrastructure.email.ResendEmailProviderClient;
import in.bachatsetu.backend.infrastructure.email.RetryingEmailSenderAdapter;
import in.bachatsetu.backend.infrastructure.email.SendGridEmailProviderClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EmailInfrastructureConfigTest {

    // Every provider's keys are always present (blank where not selected) — matching how
    // application.yml declares all three provider blocks unconditionally with `${VAR:}`
    // defaults. Spring's constructor-binding leaves a nested record entirely unbound (not
    // "bound with blanks") when none of its own keys are present at all, so a test scenario
    // that selects one provider but omits every key for the other two would fail to bind for a
    // reason unrelated to what that scenario is actually testing.
    private static final String[] BLANK_DEFAULTS_FOR_EVERY_PROVIDER = {
        "bachatsetu.email.enabled=true",
        "bachatsetu.email.from-address=noreply@example.com",
        "bachatsetu.email.retry-count=2",
        "bachatsetu.email.connect-timeout=3s",
        "bachatsetu.email.read-timeout=5s",
        "bachatsetu.email.aws-ses.region=",
        "bachatsetu.email.aws-ses.access-key=",
        "bachatsetu.email.aws-ses.secret-key=",
        "bachatsetu.email.resend.api-key=",
        "bachatsetu.email.send-grid.api-key="
    };

    // EmailInfrastructureConfig is gated on bachatsetu.email.enabled (a deployment-mode switch,
    // see its javadoc), not on any Spring profile, so no active-profile initializer is needed.
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmailInfrastructureConfig.class)
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withPropertyValues(BLANK_DEFAULTS_FOR_EVERY_PROVIDER);

    @Test
    void wiresTheAwsSesClientAndAWorkingEmailSenderWhenAwsSesIsSelected() {
        contextRunner
                .withPropertyValues(
                        "bachatsetu.email.provider=AWS_SES",
                        "bachatsetu.email.aws-ses.region=ap-south-1",
                        "bachatsetu.email.aws-ses.access-key=key",
                        "bachatsetu.email.aws-ses.secret-key=secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(EmailProviderClient.class)
                            .isInstanceOf(AwsSesEmailProviderClient.class);
                    assertThat(context).getBean(EmailSenderPort.class)
                            .isInstanceOf(RetryingEmailSenderAdapter.class);
                    assertThat(context).getBean(EmailProviderHealthTracker.class).isNotNull();
                    assertThat(context).getBean(HealthIndicator.class).isNotNull();
                });
    }

    @Test
    void wiresTheResendClientWhenResendIsSelected() {
        contextRunner
                .withPropertyValues(
                        "bachatsetu.email.provider=RESEND",
                        "bachatsetu.email.resend.api-key=key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(EmailProviderClient.class)
                            .isInstanceOf(ResendEmailProviderClient.class);
                });
    }

    @Test
    void wiresTheSendGridClientWhenSendGridIsSelected() {
        contextRunner
                .withPropertyValues(
                        "bachatsetu.email.provider=SENDGRID",
                        "bachatsetu.email.send-grid.api-key=key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(EmailProviderClient.class)
                            .isInstanceOf(SendGridEmailProviderClient.class);
                });
    }

    @Test
    void failsFastWhenTheSelectedProvidersSecretsAreMissing() {
        contextRunner
                .withPropertyValues("bachatsetu.email.provider=AWS_SES")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contributesNoBeansWhenEmailIsNotEnabled() {
        // The MVP-mode side of the exactly-one-EmailSenderPort-bean contract — see
        // LocalEmailSenderConfigTest for the mirror-image assertion.
        contextRunner
                .withPropertyValues("bachatsetu.email.enabled=false", "bachatsetu.email.provider=AWS_SES")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(EmailSenderPort.class);
                    assertThat(context).doesNotHaveBean(EmailProviderClient.class);
                });
    }
}
