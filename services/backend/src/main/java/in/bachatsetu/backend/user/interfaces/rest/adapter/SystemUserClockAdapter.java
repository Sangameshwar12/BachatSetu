package in.bachatsetu.backend.user.interfaces.rest.adapter;

import in.bachatsetu.backend.user.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Delegates application time to an injected {@link Clock}. */
public final class SystemUserClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemUserClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
