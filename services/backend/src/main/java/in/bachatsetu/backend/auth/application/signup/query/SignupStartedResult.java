package in.bachatsetu.backend.auth.application.signup.query;

import in.bachatsetu.backend.auth.domain.model.UserId;
import java.time.Instant;
import java.util.Objects;

/** Confirms an account was created and an OTP was dispatched to the given mobile number. */
public record SignupStartedResult(UserId userId, String mobileNumber, Instant otpExpiresAt) {

    public SignupStartedResult {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(mobileNumber, "mobileNumber must not be null");
        Objects.requireNonNull(otpExpiresAt, "otpExpiresAt must not be null");
    }
}
