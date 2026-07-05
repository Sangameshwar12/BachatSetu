package in.bachatsetu.backend.auth.application.port;

import java.time.Instant;

/** Supplies application time without binding use cases to a system clock. */
@FunctionalInterface
public interface ClockPort {

    Instant now();
}
