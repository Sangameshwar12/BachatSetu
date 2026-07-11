package in.bachatsetu.backend.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailProviderHealthTrackerTest {

    @Test
    void reportsUnknownBeforeAnySendIsAttempted() {
        EmailProviderHealthTracker tracker = new EmailProviderHealthTracker();

        assertThat(tracker.status()).isEqualTo(EmailProviderHealthStatus.UNKNOWN);
        assertThat(tracker.consecutiveFailures()).isZero();
    }

    @Test
    void reportsUpAfterASuccessAndResetsTheFailureCount() {
        EmailProviderHealthTracker tracker = new EmailProviderHealthTracker();

        tracker.recordFailure();
        tracker.recordSuccess();

        assertThat(tracker.status()).isEqualTo(EmailProviderHealthStatus.UP);
        assertThat(tracker.consecutiveFailures()).isZero();
    }

    @Test
    void staysUpForFewerThanThreeConsecutiveFailures() {
        EmailProviderHealthTracker tracker = new EmailProviderHealthTracker();

        tracker.recordFailure();
        tracker.recordFailure();

        assertThat(tracker.status()).isEqualTo(EmailProviderHealthStatus.UP);
        assertThat(tracker.consecutiveFailures()).isEqualTo(2);
    }

    @Test
    void reportsDownAtThreeConsecutiveFailures() {
        EmailProviderHealthTracker tracker = new EmailProviderHealthTracker();

        tracker.recordFailure();
        tracker.recordFailure();
        tracker.recordFailure();

        assertThat(tracker.status()).isEqualTo(EmailProviderHealthStatus.DOWN);
        assertThat(tracker.consecutiveFailures()).isEqualTo(3);
    }
}
