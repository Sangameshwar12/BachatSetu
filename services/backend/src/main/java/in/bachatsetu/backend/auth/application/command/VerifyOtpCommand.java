package in.bachatsetu.backend.auth.application.command;

import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Submits an OTP candidate for verification. */
public record VerifyOtpCommand(
        UserId userId,
        OtpPurpose purpose,
        OtpCode code,
        AggregateId actorId) {

    public VerifyOtpCommand {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(purpose, "OTP purpose must not be null");
        Objects.requireNonNull(code, "OTP code must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
