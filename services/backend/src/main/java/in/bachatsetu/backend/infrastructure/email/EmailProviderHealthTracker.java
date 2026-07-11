package in.bachatsetu.backend.infrastructure.email;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks the outcome of recent email send attempts so {@link EmailProviderHealthIndicator} can
 * report real provider health rather than just "is this configured". Thread-safe: send attempts
 * happen on request-handling threads, health reads happen on the actuator endpoint's own thread.
 */
public final class EmailProviderHealthTracker {

    /** Consecutive failures at or beyond this count are reported as DOWN rather than UP. */
    static final int DOWN_THRESHOLD = 3;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<EmailProviderHealthStatus> lastKnownStatus =
            new AtomicReference<>(EmailProviderHealthStatus.UNKNOWN);

    public void recordSuccess() {
        consecutiveFailures.set(0);
        lastKnownStatus.set(EmailProviderHealthStatus.UP);
    }

    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        lastKnownStatus.set(
                failures >= DOWN_THRESHOLD ? EmailProviderHealthStatus.DOWN : EmailProviderHealthStatus.UP);
    }

    public EmailProviderHealthStatus status() {
        return lastKnownStatus.get();
    }

    public int consecutiveFailures() {
        return consecutiveFailures.get();
    }
}
