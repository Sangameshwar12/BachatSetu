package in.bachatsetu.backend.auth.application.port;

import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;

/**
 * Publishes an {@link OtpApplicationEvent} for any interested listener. Kept separate from a repository
 * save: OTP verification succeeds or fails purely on its own terms, and publishing is a side effect other
 * modules (for example Audit) can react to without this module ever depending on them.
 */
@FunctionalInterface
public interface OtpEventPublisherPort {

    void publish(OtpApplicationEvent event);
}
