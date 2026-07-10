package in.bachatsetu.backend.platformoperations.interfaces.rest.adapter;

import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class SystemPlatformOperationsClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemPlatformOperationsClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
