package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** UTC-capable system clock adapter for token decisions. */
public final class SystemTokenClockAdapter implements TokenClockPort {

    private final Clock clock;

    public SystemTokenClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
