package in.bachatsetu.backend.infrastructure.email.config;

import in.bachatsetu.backend.infrastructure.email.EmailProviderType;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly typed email provider configuration. Every secret defaults to an empty string via
 * {@code ${ENV_VAR:}} placeholders in {@code application.yml} — never a hardcoded value — and the
 * compact constructor fails application startup immediately if the selected provider's required
 * secrets are blank, or if {@code fromAddress} is blank, rather than deferring the failure to the
 * first email send attempt. Mirrors {@code SmsProviderProperties} exactly.
 */
@ConfigurationProperties(prefix = "bachatsetu.email")
public record EmailProviderProperties(
        EmailProviderType provider,
        String fromAddress,
        String replyTo,
        int retryCount,
        Duration connectTimeout,
        Duration readTimeout,
        AwsSes awsSes,
        Resend resend,
        SendGrid sendGrid) {

    private static final int MAXIMUM_RETRY_COUNT = 5;

    public EmailProviderProperties {
        Objects.requireNonNull(provider, "email provider must not be null");
        fromAddress = Objects.requireNonNullElse(fromAddress, "");
        replyTo = Objects.requireNonNullElse(replyTo, fromAddress);
        requireNonBlank(fromAddress, "EMAIL_FROM_ADDRESS");
        if (retryCount < 0 || retryCount > MAXIMUM_RETRY_COUNT) {
            throw new IllegalArgumentException("email retry count must be between 0 and 5");
        }
        Objects.requireNonNull(connectTimeout, "email connect timeout must not be null");
        Objects.requireNonNull(readTimeout, "email read timeout must not be null");
        Objects.requireNonNull(awsSes, "AWS SES configuration must not be null");
        Objects.requireNonNull(resend, "Resend configuration must not be null");
        Objects.requireNonNull(sendGrid, "SendGrid configuration must not be null");
        switch (provider) {
            case AWS_SES -> awsSes.requireConfigured();
            case RESEND -> resend.requireConfigured();
            case SENDGRID -> sendGrid.requireConfigured();
        }
    }

    private static void requireNonBlank(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Refusing to start: " + variableName + " is required to send real email.");
        }
    }

    public record AwsSes(String region, String accessKey, String secretKey) {
        public AwsSes {
            region = Objects.requireNonNullElse(region, "");
            accessKey = Objects.requireNonNullElse(accessKey, "");
            secretKey = Objects.requireNonNullElse(secretKey, "");
        }

        void requireConfigured() {
            requireNonBlank(region, "AWS_SES_REGION");
            requireNonBlank(accessKey, "AWS_ACCESS_KEY");
            requireNonBlank(secretKey, "AWS_SECRET_KEY");
        }
    }

    public record Resend(String apiKey) {
        public Resend {
            apiKey = Objects.requireNonNullElse(apiKey, "");
        }

        void requireConfigured() {
            requireNonBlank(apiKey, "RESEND_API_KEY");
        }
    }

    public record SendGrid(String apiKey) {
        public SendGrid {
            apiKey = Objects.requireNonNullElse(apiKey, "");
        }

        void requireConfigured() {
            requireNonBlank(apiKey, "SENDGRID_API_KEY");
        }
    }
}
