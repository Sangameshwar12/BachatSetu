package in.bachatsetu.backend.auth.application.login.command;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import java.util.Objects;

/** Starts a returning-user login: looks up the account by mobile number and dispatches a sign-in OTP. */
public record StartLoginCommand(MobileNumber mobileNumber) {

    public StartLoginCommand {
        Objects.requireNonNull(mobileNumber, "mobileNumber must not be null");
    }
}
