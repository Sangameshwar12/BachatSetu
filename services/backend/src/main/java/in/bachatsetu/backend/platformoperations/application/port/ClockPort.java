package in.bachatsetu.backend.platformoperations.application.port;

import java.time.Instant;

@FunctionalInterface
public interface ClockPort {

    Instant now();
}
