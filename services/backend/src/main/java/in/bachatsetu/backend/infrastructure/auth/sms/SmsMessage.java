package in.bachatsetu.backend.infrastructure.auth.sms;

import java.util.Objects;

/**
 * A single outbound OTP SMS, already rendered for a provider to send. {@code toE164} is the
 * full {@code +<countrycode><number>} destination; each provider client reformats it to
 * whatever shape that provider's API expects. Neither field is ever logged.
 */
public record SmsMessage(String toE164, String otpCode, String messageBody) {

    public SmsMessage {
        Objects.requireNonNull(toE164, "destination number must not be null");
        Objects.requireNonNull(otpCode, "OTP code must not be null");
        Objects.requireNonNull(messageBody, "message body must not be null");
    }
}
