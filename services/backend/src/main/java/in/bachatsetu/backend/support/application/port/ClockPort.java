package in.bachatsetu.backend.support.application.port;

import java.time.Instant;

@FunctionalInterface
public interface ClockPort {

    Instant now();
}
