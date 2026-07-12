package in.bachatsetu.backend.auth.application.login.command;

import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.util.Objects;

/** Completes a returning-user login: verifies the sign-in OTP and issues tokens. */
public record CompleteLoginCommand(UserId userId, OtpCode code) {

    public CompleteLoginCommand {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(code, "code must not be null");
    }
}
