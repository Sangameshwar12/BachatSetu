package in.bachatsetu.backend.auth.application.event;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OtpResent(
        UUID eventId,
        AggregateId verificationId,
        AggregateId previousVerificationId,
        UserId userId,
        OtpPurpose purpose,
        Instant occurredAt) implements OtpApplicationEvent {

    public OtpResent {
        OtpApplicationEvent.validate(eventId, verificationId, userId, purpose, occurredAt);
        Objects.requireNonNull(previousVerificationId, "previous verification id must not be null");
    }
}
