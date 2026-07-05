package in.bachatsetu.backend.auth.application.token.port;

import java.time.Instant;

/** Time source dedicated to token issue and validation decisions. */
@FunctionalInterface
public interface TokenClockPort {

    Instant now();
}
