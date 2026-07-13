package in.bachatsetu.backend.infrastructure.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.infrastructure.auth.sms.Fast2SmsSmsProviderClient;
import in.bachatsetu.backend.infrastructure.auth.sms.Msg91SmsProviderClient;
import in.bachatsetu.backend.infrastructure.auth.sms.SmsOtpSenderAdapter;
import in.bachatsetu.backend.infrastructure.auth.sms.SmsProviderClient;
import in.bachatsetu.backend.infrastructure.auth.sms.SmsProviderHealthTracker;
import in.bachatsetu.backend.infrastructure.auth.sms.TwilioSmsProviderClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SmsInfrastructureConfigTest {

    // Every provider's keys are always present (blank where not selected) — matching how
    // application.yml declares all three provider blocks unconditionally with `${VAR:}`
    // defaults. Spring's constructor-binding leaves a nested record entirely unbound (not
    // "bound with blanks") when none of its own keys are present at all, so a test scenario
    // that selects one provider but omits every key for the other two would fail to bind
    // for a reason unrelated to what that scenario is actually testing.
    private static final String[] BLANK_DEFAULTS_FOR_EVERY_PROVIDER = {
        "bachatsetu.sms.enabled=true",
        "bachatsetu.sms.retry-count=2",
        "bachatsetu.sms.connect-timeout=3s",
        "bachatsetu.sms.read-timeout=5s",
        "bachatsetu.sms.msg91.auth-key=",
        "bachatsetu.sms.msg91.template-id=",
        "bachatsetu.sms.msg91.sender-id=",
        "bachatsetu.sms.fast2sms.api-key=",
        "bachatsetu.sms.twilio.account-sid=",
        "bachatsetu.sms.twilio.auth-token=",
        "bachatsetu.sms.twilio.phone-number="
    };

    // SmsInfrastructureConfig is gated on bachatsetu.sms.enabled (a deployment-mode switch,
    // see its javadoc), not on any Spring profile, so no active-profile initializer is needed here.
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SmsInfrastructureConfig.class)
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withPropertyValues(BLANK_DEFAULTS_FOR_EVERY_PROVIDER);

    @Test
    void wiresTheMsg91ClientAndAWorkingOtpSenderWhenMsg91IsSelected() {
        contextRunner
                .withPropertyValues(
                        "bachatsetu.sms.provider=MSG91",
                        "bachatsetu.sms.msg91.auth-key=key",
                        "bachatsetu.sms.msg91.template-id=template",
                        "bachatsetu.sms.msg91.sender-id=SENDER")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(SmsProviderClient.class).isInstanceOf(Msg91SmsProviderClient.class);
                    assertThat(context).getBean(OtpSenderPort.class).isInstanceOf(SmsOtpSenderAdapter.class);
                    assertThat(context).getBean(SmsProviderHealthTracker.class).isNotNull();
                    assertThat(context).getBean(HealthIndicator.class).isNotNull();
                });
    }

    @Test
    void wiresTheFast2SmsClientWhenFast2SmsIsSelected() {
        contextRunner
                .withPropertyValues(
                        "bachatsetu.sms.provider=FAST2SMS",
                        "bachatsetu.sms.fast2sms.api-key=key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(SmsProviderClient.class)
                            .isInstanceOf(Fast2SmsSmsProviderClient.class);
                });
    }

    @Test
    void wiresTheTwilioClientWhenTwilioIsSelected() {
        contextRunner
                .withPropertyValues(
                        "bachatsetu.sms.provider=TWILIO",
                        "bachatsetu.sms.twilio.account-sid=sid",
                        "bachatsetu.sms.twilio.auth-token=token",
                        "bachatsetu.sms.twilio.phone-number=+15005550006")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(SmsProviderClient.class).isInstanceOf(TwilioSmsProviderClient.class);
                });
    }

    @Test
    void failsFastWhenTheSelectedProvidersSecretsAreMissing() {
        contextRunner
                .withPropertyValues("bachatsetu.sms.provider=MSG91")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contributesNoBeansWhenSmsIsNotEnabled() {
        // The MVP-mode side of the exactly-one-OtpSenderPort-bean contract — see
        // AuthenticationInfrastructureConfigTest for the mirror-image assertion on
        // LocalOtpSenderConfig.
        contextRunner
                .withPropertyValues("bachatsetu.sms.enabled=false", "bachatsetu.sms.provider=MSG91")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(OtpSenderPort.class);
                    assertThat(context).doesNotHaveBean(SmsProviderClient.class);
                });
    }
}
