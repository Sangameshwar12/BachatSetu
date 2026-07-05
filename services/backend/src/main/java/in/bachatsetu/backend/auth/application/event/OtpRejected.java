package in.bachatsetu.backend.auth.application.event;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OtpRejected(
        UUID eventId,
        AggregateId verificationId,
        UserId userId,
        OtpPurpose purpose,
        OtpRejectionReason reason,
        Instant occurredAt) implements OtpApplicationEvent {

    public OtpRejected {
        OtpApplicationEvent.validate(eventId, verificationId, userId, purpose, occurredAt);
        Objects.requireNonNull(reason, "OTP rejection reason must not be null");
    }
}
