package in.bachatsetu.backend.payment.interfaces.rest.adapter;

import in.bachatsetu.backend.payment.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Delegates application time to an injected {@link Clock}. */
public final class SystemPaymentClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemPaymentClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
