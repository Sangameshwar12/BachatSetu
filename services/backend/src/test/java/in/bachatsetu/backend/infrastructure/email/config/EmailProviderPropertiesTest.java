package in.bachatsetu.backend.infrastructure.email.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.infrastructure.email.EmailProviderType;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EmailProviderPropertiesTest {

    private static final EmailProviderProperties.AwsSes CONFIGURED_AWS_SES =
            new EmailProviderProperties.AwsSes("ap-south-1", "access-key", "secret-key");
    private static final EmailProviderProperties.AwsSes BLANK_AWS_SES =
            new EmailProviderProperties.AwsSes("", "", "");
    private static final EmailProviderProperties.Resend CONFIGURED_RESEND =
            new EmailProviderProperties.Resend("re_api_key");
    private static final EmailProviderProperties.Resend BLANK_RESEND = new EmailProviderProperties.Resend("");
    private static final EmailProviderProperties.SendGrid CONFIGURED_SENDGRID =
            new EmailProviderProperties.SendGrid("SG.api_key");
    private static final EmailProviderProperties.SendGrid BLANK_SENDGRID = new EmailProviderProperties.SendGrid("");

    @Test
    void startsWhenTheSelectedProviderIsFullyConfigured() {
        assertThatNoException().isThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.AWS_SES, "noreply@example.com", "support@example.com", 2,
                Duration.ofSeconds(3), Duration.ofSeconds(5), CONFIGURED_AWS_SES, BLANK_RESEND, BLANK_SENDGRID));
        assertThatNoException().isThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.RESEND, "noreply@example.com", "support@example.com", 2,
                Duration.ofSeconds(3), Duration.ofSeconds(5), BLANK_AWS_SES, CONFIGURED_RESEND, BLANK_SENDGRID));
        assertThatNoException().isThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.SENDGRID, "noreply@example.com", "support@example.com", 2,
                Duration.ofSeconds(3), Duration.ofSeconds(5), BLANK_AWS_SES, BLANK_RESEND, CONFIGURED_SENDGRID));
    }

    @Test
    void refusesToStartWhenTheSelectedProviderIsMissingItsSecrets() {
        assertThatThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.AWS_SES, "noreply@example.com", "support@example.com", 2,
                Duration.ofSeconds(3), Duration.ofSeconds(5), BLANK_AWS_SES, CONFIGURED_RESEND, CONFIGURED_SENDGRID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AWS_SES_REGION");

        assertThatThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.RESEND, "noreply@example.com", "support@example.com", 2,
                Duration.ofSeconds(3), Duration.ofSeconds(5), CONFIGURED_AWS_SES, BLANK_RESEND, CONFIGURED_SENDGRID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RESEND_API_KEY");

        assertThatThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.SENDGRID, "noreply@example.com", "support@example.com", 2,
                Duration.ofSeconds(3), Duration.ofSeconds(5), CONFIGURED_AWS_SES, CONFIGURED_RESEND, BLANK_SENDGRID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SENDGRID_API_KEY");
    }

    @Test
    void refusesToStartWithoutAFromAddressRegardlessOfProvider() {
        assertThatThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.AWS_SES, "", "support@example.com", 2,
                Duration.ofSeconds(3), Duration.ofSeconds(5), CONFIGURED_AWS_SES, BLANK_RESEND, BLANK_SENDGRID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMAIL_FROM_ADDRESS");
    }

    @Test
    void defaultsReplyToToFromAddressWhenNotSupplied() {
        EmailProviderProperties properties = new EmailProviderProperties(
                EmailProviderType.AWS_SES, "noreply@example.com", null, 2,
                Duration.ofSeconds(3), Duration.ofSeconds(5), CONFIGURED_AWS_SES, BLANK_RESEND, BLANK_SENDGRID);

        assertThat(properties.replyTo()).isEqualTo("noreply@example.com");
    }

    @Test
    void refusesANegativeOrExcessiveRetryCount() {
        assertThatThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.AWS_SES, "noreply@example.com", "support@example.com", -1,
                Duration.ofSeconds(3), Duration.ofSeconds(5), CONFIGURED_AWS_SES, BLANK_RESEND, BLANK_SENDGRID))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new EmailProviderProperties(
                EmailProviderType.AWS_SES, "noreply@example.com", "support@example.com", 6,
                Duration.ofSeconds(3), Duration.ofSeconds(5), CONFIGURED_AWS_SES, BLANK_RESEND, BLANK_SENDGRID))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
