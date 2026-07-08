package in.bachatsetu.backend.audit.application.port;

import java.time.Instant;

/** Supplies application time without coupling services to the system clock. */
@FunctionalInterface
public interface ClockPort {

    Instant now();
}
