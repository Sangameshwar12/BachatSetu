package in.bachatsetu.backend.auth.application.command;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests an initial OTP challenge. */
public record GenerateOtpCommand(UserId userId, OtpPurpose purpose, AggregateId actorId) {

    public GenerateOtpCommand {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(purpose, "OTP purpose must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
