package in.bachatsetu.backend.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class EmailProviderHealthIndicatorTest {

    @Test
    void reportsUnknownWithNoSecretsBeforeAnySendIsAttempted() {
        EmailProviderHealthTracker tracker = new EmailProviderHealthTracker();
        EmailProviderHealthIndicator indicator = new EmailProviderHealthIndicator(EmailProviderType.AWS_SES, tracker);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("provider", EmailProviderType.AWS_SES);
        assertThat(health.getDetails()).doesNotContainKeys(
                "accessKey", "secretKey", "apiKey", "AWS_SECRET_KEY", "RESEND_API_KEY", "SENDGRID_API_KEY");
    }

    @Test
    void reportsUpAfterASuccessfulSend() {
        EmailProviderHealthTracker tracker = new EmailProviderHealthTracker();
        tracker.recordSuccess();
        EmailProviderHealthIndicator indicator = new EmailProviderHealthIndicator(EmailProviderType.RESEND, tracker);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownAfterThreeConsecutiveFailures() {
        EmailProviderHealthTracker tracker = new EmailProviderHealthTracker();
        tracker.recordFailure();
        tracker.recordFailure();
        tracker.recordFailure();
        EmailProviderHealthIndicator indicator = new EmailProviderHealthIndicator(EmailProviderType.SENDGRID, tracker);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("consecutiveFailures", 3);
    }
}
