package in.bachatsetu.backend.storage.interfaces.rest.adapter;

import in.bachatsetu.backend.storage.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Delegates application time to an injected {@link Clock}. */
public final class SystemStorageClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemStorageClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
