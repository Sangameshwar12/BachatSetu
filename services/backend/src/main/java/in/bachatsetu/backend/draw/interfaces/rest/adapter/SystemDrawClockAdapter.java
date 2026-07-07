package in.bachatsetu.backend.draw.interfaces.rest.adapter;

import in.bachatsetu.backend.draw.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Delegates application time to an injected {@link Clock}. */
public final class SystemDrawClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemDrawClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
