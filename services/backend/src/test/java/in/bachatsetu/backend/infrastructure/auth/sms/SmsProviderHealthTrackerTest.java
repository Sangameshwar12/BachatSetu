package in.bachatsetu.backend.infrastructure.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmsProviderHealthTrackerTest {

    @Test
    void reportsUnknownBeforeAnySendIsAttempted() {
        SmsProviderHealthTracker tracker = new SmsProviderHealthTracker();

        assertThat(tracker.status()).isEqualTo(SmsProviderHealthStatus.UNKNOWN);
        assertThat(tracker.consecutiveFailures()).isZero();
    }

    @Test
    void reportsUpAfterASuccessAndResetsTheFailureCount() {
        SmsProviderHealthTracker tracker = new SmsProviderHealthTracker();

        tracker.recordFailure();
        tracker.recordSuccess();

        assertThat(tracker.status()).isEqualTo(SmsProviderHealthStatus.UP);
        assertThat(tracker.consecutiveFailures()).isZero();
    }

    @Test
    void staysUpForFewerThanThreeConsecutiveFailures() {
        SmsProviderHealthTracker tracker = new SmsProviderHealthTracker();

        tracker.recordFailure();
        tracker.recordFailure();

        assertThat(tracker.status()).isEqualTo(SmsProviderHealthStatus.UP);
        assertThat(tracker.consecutiveFailures()).isEqualTo(2);
    }

    @Test
    void reportsDownAtThreeConsecutiveFailures() {
        SmsProviderHealthTracker tracker = new SmsProviderHealthTracker();

        tracker.recordFailure();
        tracker.recordFailure();
        tracker.recordFailure();

        assertThat(tracker.status()).isEqualTo(SmsProviderHealthStatus.DOWN);
        assertThat(tracker.consecutiveFailures()).isEqualTo(3);
    }
}
