package in.bachatsetu.backend.infrastructure.email;

import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Exposes {@link EmailProviderHealthTracker}'s state under {@code /actuator/health/emailProvider}.
 * Reports only the provider name and a failure count — never a secret or a recipient address.
 */
public final class EmailProviderHealthIndicator implements HealthIndicator {

    private final EmailProviderType provider;
    private final EmailProviderHealthTracker tracker;

    public EmailProviderHealthIndicator(EmailProviderType provider, EmailProviderHealthTracker tracker) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.tracker = Objects.requireNonNull(tracker, "health tracker must not be null");
    }

    @Override
    public Health health() {
        Health.Builder builder = switch (tracker.status()) {
            case UP -> Health.up();
            case DOWN -> Health.down();
            case UNKNOWN -> Health.unknown();
        };
        return builder
                .withDetail("provider", provider)
                .withDetail("consecutiveFailures", tracker.consecutiveFailures())
                .build();
    }
}
