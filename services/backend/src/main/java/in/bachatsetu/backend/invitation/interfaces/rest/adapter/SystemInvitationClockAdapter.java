package in.bachatsetu.backend.invitation.interfaces.rest.adapter;

import in.bachatsetu.backend.invitation.application.port.ClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class SystemInvitationClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemInvitationClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
