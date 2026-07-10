package in.bachatsetu.backend.invitation.application.port;

import java.time.Instant;

@FunctionalInterface
public interface ClockPort {

    Instant now();
}
