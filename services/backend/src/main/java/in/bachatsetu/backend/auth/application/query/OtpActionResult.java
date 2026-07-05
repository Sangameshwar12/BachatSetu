package in.bachatsetu.backend.auth.application.query;

import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import java.util.List;
import java.util.Objects;

/** Result returned by OTP commands without exposing codes or hashes. */
public record OtpActionResult(OtpChallengeView challenge, List<OtpApplicationEvent> events) {

    public OtpActionResult {
        Objects.requireNonNull(challenge, "OTP challenge must not be null");
        events = List.copyOf(Objects.requireNonNull(events, "OTP events must not be null"));
    }

    public static OtpActionResult from(
            OtpVerification verification,
            List<OtpApplicationEvent> events) {
        return new OtpActionResult(OtpChallengeView.from(verification), events);
    }
}
