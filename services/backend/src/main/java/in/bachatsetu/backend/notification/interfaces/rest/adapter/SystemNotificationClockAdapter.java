package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import in.bachatsetu.backend.notification.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Delegates application time to an injected {@link Clock}. */
public final class SystemNotificationClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemNotificationClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
