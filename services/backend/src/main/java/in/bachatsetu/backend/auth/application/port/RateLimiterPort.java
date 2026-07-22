package in.bachatsetu.backend.auth.application.port;

import java.time.Duration;

/** Outbound port for a fixed-window request counter, used to throttle abuse-prone actions such as OTP generation. */
public interface RateLimiterPort {

    /**
     * Records one attempt under {@code key} and reports whether it is still within {@code
     * maxAttempts} for the current {@code window}. The window starts on the first attempt for a
     * given key and resets once it elapses.
     */
    boolean tryConsume(String key, int maxAttempts, Duration window);
}
