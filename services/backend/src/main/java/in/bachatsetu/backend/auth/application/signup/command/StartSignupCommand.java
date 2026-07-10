package in.bachatsetu.backend.auth.application.signup.command;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.shared.domain.Email;
import java.util.Objects;

/** Starts self-registration: creates the account (pending verification) and dispatches an OTP. */
public record StartSignupCommand(
        String givenName,
        String familyName,
        MobileNumber mobileNumber,
        Email email,
        String preferredLanguage,
        boolean acceptedTerms) {

    public StartSignupCommand {
        Objects.requireNonNull(givenName, "givenName must not be null");
        Objects.requireNonNull(mobileNumber, "mobileNumber must not be null");
        Objects.requireNonNull(preferredLanguage, "preferredLanguage must not be null");
    }
}
