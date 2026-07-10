package in.bachatsetu.backend.support.interfaces.rest.adapter;

import in.bachatsetu.backend.support.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class SystemSupportClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemSupportClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
