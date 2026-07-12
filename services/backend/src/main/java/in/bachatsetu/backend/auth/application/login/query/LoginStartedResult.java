package in.bachatsetu.backend.auth.application.login.query;

import in.bachatsetu.backend.auth.domain.model.UserId;
import java.time.Instant;
import java.util.Objects;

/** Confirms a sign-in OTP was dispatched to the given mobile number. */
public record LoginStartedResult(UserId userId, String mobileNumber, Instant otpExpiresAt) {

    public LoginStartedResult {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(mobileNumber, "mobileNumber must not be null");
        Objects.requireNonNull(otpExpiresAt, "otpExpiresAt must not be null");
    }
}
