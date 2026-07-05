package in.bachatsetu.backend.auth.application.event;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.UUID;

public record OtpExpired(
        UUID eventId,
        AggregateId verificationId,
        UserId userId,
        OtpPurpose purpose,
        Instant occurredAt) implements OtpApplicationEvent {

    public OtpExpired {
        OtpApplicationEvent.validate(eventId, verificationId, userId, purpose, occurredAt);
    }
}
