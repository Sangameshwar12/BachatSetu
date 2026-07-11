package in.bachatsetu.backend.infrastructure.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class SmsProviderHealthIndicatorTest {

    @Test
    void reportsUnknownWithNoSecretsBeforeAnySendIsAttempted() {
        SmsProviderHealthTracker tracker = new SmsProviderHealthTracker();
        SmsProviderHealthIndicator indicator = new SmsProviderHealthIndicator(SmsProviderType.MSG91, tracker);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("provider", SmsProviderType.MSG91);
        assertThat(health.getDetails()).doesNotContainKeys(
                "authKey", "apiKey", "authToken", "MSG91_AUTH_KEY", "secret");
    }

    @Test
    void reportsUpAfterASuccessfulSend() {
        SmsProviderHealthTracker tracker = new SmsProviderHealthTracker();
        tracker.recordSuccess();
        SmsProviderHealthIndicator indicator = new SmsProviderHealthIndicator(SmsProviderType.TWILIO, tracker);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownAfterThreeConsecutiveFailures() {
        SmsProviderHealthTracker tracker = new SmsProviderHealthTracker();
        tracker.recordFailure();
        tracker.recordFailure();
        tracker.recordFailure();
        SmsProviderHealthIndicator indicator = new SmsProviderHealthIndicator(SmsProviderType.FAST2SMS, tracker);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("consecutiveFailures", 3);
    }
}
