package in.bachatsetu.backend.infrastructure.auth.sms;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks the outcome of recent SMS send attempts so {@link SmsProviderHealthIndicator} can
 * report real provider health rather than just "is this configured" — a provider that is
 * correctly configured but unreachable (network outage, revoked credentials, exhausted
 * balance) should surface as {@code DOWN}, not {@code UP}. Thread-safe: send attempts happen on
 * request-handling threads, health reads happen on the actuator endpoint's own thread.
 */
public final class SmsProviderHealthTracker {

    /** Consecutive failures at or beyond this count are reported as DOWN rather than UP. */
    static final int DOWN_THRESHOLD = 3;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<SmsProviderHealthStatus> lastKnownStatus =
            new AtomicReference<>(SmsProviderHealthStatus.UNKNOWN);

    public void recordSuccess() {
        consecutiveFailures.set(0);
        lastKnownStatus.set(SmsProviderHealthStatus.UP);
    }

    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        lastKnownStatus.set(
                failures >= DOWN_THRESHOLD ? SmsProviderHealthStatus.DOWN : SmsProviderHealthStatus.UP);
    }

    public SmsProviderHealthStatus status() {
        return lastKnownStatus.get();
    }

    public int consecutiveFailures() {
        return consecutiveFailures.get();
    }
}
