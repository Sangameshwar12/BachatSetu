package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.infrastructure.auth.sms.SmsProviderType;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly typed SMS provider configuration. Every secret defaults to an empty string via
 * {@code ${ENV_VAR:}} placeholders in {@code application.yml} — never a hardcoded value — and
 * the compact constructor fails application startup immediately if the selected provider's
 * required secrets are blank, rather than deferring the failure to the first OTP send attempt.
 */
@ConfigurationProperties(prefix = "bachatsetu.sms")
public record SmsProviderProperties(
        SmsProviderType provider,
        int retryCount,
        Duration connectTimeout,
        Duration readTimeout,
        Msg91 msg91,
        Fast2Sms fast2sms,
        Twilio twilio) {

    private static final int MAXIMUM_RETRY_COUNT = 5;

    public SmsProviderProperties {
        Objects.requireNonNull(provider, "SMS provider must not be null");
        if (retryCount < 0 || retryCount > MAXIMUM_RETRY_COUNT) {
            throw new IllegalArgumentException("SMS retry count must be between 0 and 5");
        }
        Objects.requireNonNull(connectTimeout, "SMS connect timeout must not be null");
        Objects.requireNonNull(readTimeout, "SMS read timeout must not be null");
        Objects.requireNonNull(msg91, "MSG91 configuration must not be null");
        Objects.requireNonNull(fast2sms, "Fast2SMS configuration must not be null");
        Objects.requireNonNull(twilio, "Twilio configuration must not be null");
        switch (provider) {
            case MSG91 -> msg91.requireConfigured();
            case FAST2SMS -> fast2sms.requireConfigured();
            case TWILIO -> twilio.requireConfigured();
        }
    }

    private static void requireNonBlank(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Refusing to start: " + variableName
                            + " is required because SMS_PROVIDER selects this provider.");
        }
    }

    public record Msg91(String authKey, String templateId, String senderId) {
        public Msg91 {
            authKey = Objects.requireNonNullElse(authKey, "");
            templateId = Objects.requireNonNullElse(templateId, "");
            senderId = Objects.requireNonNullElse(senderId, "");
        }

        void requireConfigured() {
            requireNonBlank(authKey, "MSG91_AUTH_KEY");
            requireNonBlank(templateId, "MSG91_TEMPLATE_ID");
            requireNonBlank(senderId, "MSG91_SENDER_ID");
        }
    }

    public record Fast2Sms(String apiKey) {
        public Fast2Sms {
            apiKey = Objects.requireNonNullElse(apiKey, "");
        }

        void requireConfigured() {
            requireNonBlank(apiKey, "FAST2SMS_API_KEY");
        }
    }

    public record Twilio(String accountSid, String authToken, String phoneNumber) {
        public Twilio {
            accountSid = Objects.requireNonNullElse(accountSid, "");
            authToken = Objects.requireNonNullElse(authToken, "");
            phoneNumber = Objects.requireNonNullElse(phoneNumber, "");
        }

        void requireConfigured() {
            requireNonBlank(accountSid, "TWILIO_ACCOUNT_SID");
            requireNonBlank(authToken, "TWILIO_AUTH_TOKEN");
            requireNonBlank(phoneNumber, "TWILIO_PHONE_NUMBER");
        }
    }
}
