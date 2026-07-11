package in.bachatsetu.backend.auth.application.event;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Published by the SMS delivery infrastructure (not by an OTP application service) when every
 * configured retry attempt to reach the SMS provider has been exhausted, immediately before the
 * infrastructure adapter translates the failure into an {@code OtpApplicationException} with
 * reason {@code SMS_DELIVERY_FAILED}. Deliberately does not implement {@link OtpApplicationEvent}:
 * that interface requires a {@code verificationId}, which {@code OtpSenderPort.send(...)} is not
 * given — this event carries only what the sender actually has. Contains no OTP code, no phone
 * number, and no provider secret.
 */
public record OtpSendFailed(
        UUID eventId,
        UserId userId,
        OtpPurpose purpose,
        String provider,
        String failureReason,
        Instant occurredAt) {

    public OtpSendFailed {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(purpose, "OTP purpose must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(failureReason, "failure reason must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
