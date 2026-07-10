package in.bachatsetu.backend.auth.application.signup.command;

import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.util.Objects;

/** Completes self-registration: verifies the signup OTP, activates the account, and issues tokens. */
public record CompleteSignupCommand(UserId userId, OtpCode code) {

    public CompleteSignupCommand {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(code, "code must not be null");
    }
}
