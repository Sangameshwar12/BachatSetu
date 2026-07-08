package in.bachatsetu.backend.automation.application.port;

import java.time.Instant;

/** Supplies deterministic application time, mockable in tests without a scheduler ever waiting on a cron. */
@FunctionalInterface
public interface ClockPort {

    Instant now();
}
