package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** UTC-capable application clock backed by an injected Java clock. */
public final class SystemClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
