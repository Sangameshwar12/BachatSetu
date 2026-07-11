package in.bachatsetu.backend.infrastructure.auth.sms;

import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Exposes {@link SmsProviderHealthTracker}'s state under {@code /actuator/health/smsProvider}.
 * Reports only the provider name and a failure count — never a secret, a phone number, or an
 * OTP — matching {@code management.endpoint.health.show-details} controlling whether this
 * appears at all to an unauthenticated caller (see {@code application.yml}).
 */
public final class SmsProviderHealthIndicator implements HealthIndicator {

    private final SmsProviderType provider;
    private final SmsProviderHealthTracker tracker;

    public SmsProviderHealthIndicator(SmsProviderType provider, SmsProviderHealthTracker tracker) {
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
